package marchandivan.RoomMonitoring.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import marchandivan.RoomMonitoring.AlarmActivity;
import marchandivan.RoomMonitoring.R;
import marchandivan.RoomMonitoring.db.AlarmConfig;
import marchandivan.RoomMonitoring.db.RoomConfig;
import marchandivan.RoomMonitoring.http.RestClient;

public class MonitorRoomReceiver extends BroadcastReceiver {

    // Polling interval (in ms)
    static final int mPollingInterval = 60 * 1000; // Every minute

    // Quiet period during which the alarm won't be fired after an alert
    static final long mQuietPeriod = 30 * 60 * 1000; // 30 minutes

    // Expiration time after which the temperature measure is not valid anymore
    static final long mTempMeasureExpirationTime = 10 * 60 * 1000; // 10 minutes

    // true if alarm in activated
    private boolean mAlarmActivated = false;

    // TextView to update
    static private HashMap<String, HashMap<String, Pair<TextView, TextView> > > mTextViewListeners = new HashMap<String, HashMap<String, Pair<TextView, TextView> > >();

    // Callbacks
    static private HashMap<String, Runnable> mPreUpdateCallbacks = new HashMap<String, Runnable>();
    static private HashMap<String, Runnable> mPostUpdateCallbacks = new HashMap<String, Runnable>();

    // Rest client instance, used to access remote server
    private RestClient mRestClient;

    // Update room temp
    public static void Update(Context context) {
        MonitorRoomReceiver monitorRoomReceiver = new MonitorRoomReceiver();
        monitorRoomReceiver.update(context);
    }

    // Activate the monitoring
    public static void Activate(Context context) {
        Intent intent = new Intent(context, MonitorRoomReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + mPollingInterval,
                mPollingInterval,
                pendingIntent);
        Log.d("MonitorRoom", "Activate monitoring");

        // Enable boot receiver
        ComponentName receiver = new ComponentName(context, BootReceiver.class);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);

    }

    // Deactivate the monitoring
    public static void Deactivate(Context context) {
        Intent intent = new Intent(context, MonitorRoomReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pendingIntent);
        Log.d("MonitorRoom", "Deactivate monitoring");

        // Disable boot receiver
        ComponentName receiver = new ComponentName(context, BootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    protected class Update extends AsyncTask<Void, Void, JSONArray> {
        Context mContext;

        public Update(Context context) {
            mContext = context;
        }

        protected JSONArray doInBackground(Void... voids) {
            // Call callbacks
            for (Runnable callback : mPreUpdateCallbacks.values()) {
                callback.run();
            }

            return mRestClient.getArray("/api/v1/get/rooms");
        }

        protected void onPostExecute(JSONArray result) {
            try {
                long lastUpdate = System.currentTimeMillis();
                HashSet<String> newRooms = new HashSet<String>();
                for (int i = 0 ; i < result.length() ; i++) {
                    JSONObject room = result.getJSONObject(i);
                    String roomName = room.getString("room");
                    newRooms.add(roomName);

                    // Update Db
                    HashMap<String, RoomConfig> roomConfigs = RoomConfig.GetMap(mContext);
                    Log.d("UpdateRoom", roomName);
                    if (roomConfigs.containsKey(roomName)) {
                        roomConfigs.get(roomName).update(lastUpdate, room);
                    } else {
                        RoomConfig roomConfig = new RoomConfig(mContext, roomName, lastUpdate, room);
                        roomConfig.add();
                    }
                    // Remove room from cache if expired
                    for (RoomConfig roomConfig: roomConfigs.values()) {
                        if (!newRooms.contains(roomConfig.mRoomName) && roomConfig.hasExpired()) {
                            roomConfig.delete();
                        }
                    }
                    // Update registered views
                    Float temperature = room.has("temperature") ? Float.parseFloat(room.getString("temperature")) : null;
                    Float humidity = room.has("humidity") ? Float.parseFloat(room.getString("humidity")) : null;
                    UpdateViews(roomName, temperature, humidity);
                }

                // Call callbacks
                for (Runnable callback : mPostUpdateCallbacks.values()) {
                    callback.run();
                }

            } catch (Exception e) {
                Log.d("UpdateRoom", "Error " + e.toString());
            }
            // Check alarm
            checkAlarm(mContext);
        }
    }

    private static void UpdateViews(String room, Float temperature, Float humidity) {
        if (mTextViewListeners.containsKey(room)) {
            for(Map.Entry<String, Pair<TextView, TextView> > roomViews : mTextViewListeners.get(room).entrySet()) {
                if (temperature != null) {
                    // Temperature
                    roomViews.getValue().first.setText(String.format("%.1f F", temperature));
                }
                if (humidity != null) {
                    // Humidity
                    roomViews.getValue().second.setText(String.format("%.1f %%", humidity));
                }
            }
        }
    }

    public void update(Context context) {

        this.configure(context);

        // Update Room Temperature
        new Update(context).execute();
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        this.configure(context);

        // Update Room Temperature
        new Update(context).execute();

    }

    private void checkAlarm(Context context) {
        try {
            // Check min/max temp
            HashMap<String, RoomConfig> roomConfigs = RoomConfig.GetMap(context);
            for (RoomConfig roomConfig : roomConfigs.values()) {
                AlarmConfig alarmConfig = new AlarmConfig(context, roomConfig.mRoomName);
                if (mAlarmActivated && MeasureIsValid(roomConfig) && !inQuietPeriod(roomConfig)) {
                    for (AlarmConfig.Alarm alarm : alarmConfig.read()) {
                        if (alarm.mAlarmActive && exceedThreshold(roomConfig, alarm)) {
                            roomConfig.updateAlarm();
                            Log.d("MonitorRoom", "Firing alarm!");
                            Intent alarmIntent = new Intent(context, AlarmActivity.class);
                            Bundle args = new Bundle();
                            // Setup the alarm
                            alarmIntent.putExtra("room", roomConfig.mRoomName);
                            alarmIntent.putExtra("max_temperature", alarm.mMaxTemp);
                            alarmIntent.putExtra("min_temperature", alarm.mMinTemp);
                            alarmIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(alarmIntent);

                            // No need to start alarm several time
                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean inQuietPeriod(RoomConfig roomConfig) {
        return roomConfig.mLastAlarm + mQuietPeriod > System.currentTimeMillis();
    }

    static private boolean MeasureIsValid(RoomConfig roomConfig) {
        long currentTime = System.currentTimeMillis();
        // Measure expired?
        return roomConfig.mLastUpdate + mTempMeasureExpirationTime > currentTime;
    }

    private boolean exceedThreshold(RoomConfig roomConfig, AlarmConfig.Alarm alarm) throws JSONException {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        Integer nowMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
        Float temperature = roomConfig.mData.has("temperature") ? Float.parseFloat(roomConfig.mData.getString("temperature")) : null;
        return isAlarmEnable(alarm, nowMinutes) && temperature != null && (temperature > alarm.mMaxTemp || temperature < alarm.mMinTemp);
    }

    public boolean isAlarmEnable(AlarmConfig.Alarm alarm, Integer nowMinutes) {
        Log.d("MonitorRoomReceiver", String.valueOf(nowMinutes));
        Integer startMinutes = alarm.mStartTime.first * 60 + alarm.mStartTime.second;
        Integer stopMinutes = alarm.mStopTime.first * 60 + alarm.mStopTime.second;
        if (stopMinutes > startMinutes) {
            return nowMinutes >= startMinutes && nowMinutes <= stopMinutes;
        } else if (startMinutes > stopMinutes) {
            return nowMinutes >= startMinutes || nowMinutes <= stopMinutes;
        }
        // startMinutes == stopMinutes
        return true;
    }

    public void configure(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mRestClient = new RestClient(context);
        mRestClient.configure(sharedPreferences);

        mAlarmActivated = sharedPreferences.getBoolean("temperature_alarm", false);

    }


    static public void Register(String room, String key, TextView tempTextView, TextView humidityTextView) {
        try {

            if (!mTextViewListeners.containsKey(room)) {
                mTextViewListeners.put(room, new HashMap<String, Pair<TextView, TextView>>());
            }
            mTextViewListeners.get(room).put(key, new Pair<TextView, TextView>(tempTextView, humidityTextView));

            // Update display
            HashMap<String, RoomConfig> roomConfigHashMap = RoomConfig.GetMap(tempTextView.getContext());
            if (roomConfigHashMap.containsKey(room)) {
                RoomConfig roomConfig = roomConfigHashMap.get(room);
                Float temperature = roomConfig.mData.has("temperature") ? Float.parseFloat(roomConfig.mData.getString("temperature")) : null;
                Float humidity = roomConfig.mData.has("humidity") ? Float.parseFloat(roomConfig.mData.getString("humidity")) : null;
                // Measure expired?
                if (MeasureIsValid(roomConfig)) {
                    UpdateViews(room, temperature, humidity);
                }
            }

        }
        catch (Exception e) {

        }
    }

    static public void Unregister(String room, String key) {
        if (mTextViewListeners.containsKey(room) &&  mTextViewListeners.get(room).containsKey(key)) {
            mTextViewListeners.get(room).remove(key);
        }
    }

    static public void AddPostUpdateCallback(String room, String key, Runnable callback) {
        mPostUpdateCallbacks.put(room + "_" + key, callback);
    }

    static public void RemovePostUpdateCallback(String room, String key) {
        if (mPostUpdateCallbacks.containsKey(room + "_" + key)) {
            mPostUpdateCallbacks.remove(room + "_" + key);
        }
    }

    static public void AddPreUpdateCallback(String room, String key, Runnable callback) {
        mPreUpdateCallbacks.put(room + "_" + key, callback);
    }

    static public void RemovePreUpdateCallback(String room, String key) {
        if (mPreUpdateCallbacks.containsKey(room + "_" + key)) {
            mPreUpdateCallbacks.remove(room + "_" + key);
        }
    }

}
