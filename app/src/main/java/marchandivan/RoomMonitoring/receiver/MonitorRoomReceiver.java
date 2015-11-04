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
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Time;
import java.util.Calendar;
import java.util.HashMap;

import marchandivan.RoomMonitoring.AlarmActivity;
import marchandivan.RoomMonitoring.db.AlarmConfig;
import marchandivan.RoomMonitoring.db.RoomConfig;
import marchandivan.RoomMonitoring.http.RestClient;

public class MonitorRoomReceiver extends BroadcastReceiver {

    // Polling interval (in ms)
    static final int mPollingInterval = 60 * 1000; // Every minute

    // Quiet period during which the alarm won't be fired after an alert
    static final long mQuietPeriod = 30 * 60 * 1000; // 30 minutes

    // true if alarm in activated
    private boolean mAlarmActivated = false;

    // Map containing the last Temperature/Humidity values retrieved from server
    static HashMap<String, JSONObject> mRoomMap = new HashMap<String, JSONObject>();

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
            return mRestClient.getArray("/api/v1/get/rooms");
        }

        protected void onPostExecute(JSONArray result) {
            try {
                long lastUpdate = System.currentTimeMillis();
                for (int i = 0 ; i < result.length() ; i++) {
                    JSONObject room = result.getJSONObject(i);
                    String roomName = room.getString("room");
                    mRoomMap.put(roomName, room);
                    HashMap<String, RoomConfig> roomConfigs = RoomConfig.GetMap(mContext);
                    if (roomConfigs.containsKey(roomName)) {
                        Log.d("UpdateRoom", roomName);
                        roomConfigs.get(roomName).update(lastUpdate);
                    }
                }
            } catch (Exception e) {
                Log.d("UpdateRoom", "Error " + e.toString());
            }
        }
    }

    static public HashMap<String, JSONObject> GetRooms() {
        return mRoomMap;
    }

    public void update(Context context) {

        this.configure(context);

        // Update Room Temperature
        new Update(context).execute();
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        try {
            this.configure(context);

            // Update Room Temperature
            new Update(context).execute();

            // Check min/max temp
            HashMap<String, RoomConfig> roomConfigs = RoomConfig.GetMap(context);
            for (RoomConfig roomConfig : roomConfigs.values()) {
                AlarmConfig alarmConfig = new AlarmConfig(context, roomConfig.mRoomName);
                if (mAlarmActivated && !inQuietPeriod(roomConfig)) {
                    for (AlarmConfig.Alarm alarm : alarmConfig.read()) {
                        if (alarm.mAlarmActive && exceedThreshold(roomConfig.mRoomName, alarm)) {
                            roomConfig.updateAlarm();
                            Log.d("MonitorRoom", "Firing alarm!");
                            Intent alarmIntent = new Intent(context, AlarmActivity.class);
                            alarmIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(alarmIntent);

                            // No need to start alarm several time
                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {

        }
    }

    private boolean inQuietPeriod(RoomConfig roomConfig) {
        return roomConfig.mLastAlarm + mQuietPeriod > System.currentTimeMillis();
    }

    private boolean exceedThreshold(String roomName, AlarmConfig.Alarm alarm) throws JSONException {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        Integer nowMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
        Float temperature = mRoomMap.containsKey(roomName) ? Float.parseFloat(mRoomMap.get(roomName).getString("temperature")) : null;
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
        mRestClient = new RestClient(context.getAssets());
        mRestClient.configure(sharedPreferences);

        mAlarmActivated = sharedPreferences.getBoolean("temperature_alarm", false);

    }

}
