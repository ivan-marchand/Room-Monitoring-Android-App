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
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import marchandivan.RoomMonitoring.AlarmActivity;
import marchandivan.RoomMonitoring.db.RoomConfig;
import marchandivan.RoomMonitoring.http.RestClient;

public class MonitorRoomReceiver extends BroadcastReceiver {

    // Polling interval (in ms)
    static final int mPollingInterval = 60 * 1000; // Every minute

    // Quiet period during which the alarm won't be fired after an alert
    static final long mQuietPeriod = 30 * 60 * 1000; // 30 minutes

    // Timestamp of the last alarm fired
    private static long mLastAlarmTimeStamp = 0;

    // true if alarm in activated
    private boolean mAlarmActivated = false;

    // Max and Min temperature threshold
    private Integer mMaxTempThreshold = null;
    private Integer mMinTempThreshold = null;

    // Map containing the last Temperature/Humidity values retrieved from server
    static HashMap<String, JSONObject> mRoomMap = new HashMap<String, JSONObject>();

    // Map containing room configuration
    static HashMap<String, RoomConfig> mRoomConfigMap = new HashMap<String, RoomConfig>();

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
                SystemClock.elapsedRealtime(),
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
                long lastUpdate = SystemClock.elapsedRealtime();
                for (RoomConfig roomConfig: mRoomConfigMap.values()) {
                    for (int i = 0 ; i < result.length() ; i++) {
                        JSONObject room = result.getJSONObject(i);
                        if (room.getString("room").equals(roomConfig.mRoomName)) {
                            Log.d("UpdateRoom", roomConfig.mRoomName);
                            roomConfig.update(lastUpdate);
                            mRoomMap.put(room.getString("room"), room);
                            break;
                        }
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

    static public HashMap<String, RoomConfig> GetRoomConfigs() {
        return mRoomConfigMap;
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
            boolean inQuietPeriod = mLastAlarmTimeStamp + mQuietPeriod > SystemClock.elapsedRealtime();
            if (!inQuietPeriod) {
                Float noahTemperature = mRoomMap.containsKey("noah") ? Float.parseFloat(mRoomMap.get("noah").getString("temperature")) : null;
                if (mAlarmActivated && noahTemperature != null && (mMaxTempThreshold != null && noahTemperature > mMaxTempThreshold || mMinTempThreshold != null && noahTemperature < mMinTempThreshold)) {
                    mLastAlarmTimeStamp = SystemClock.elapsedRealtime();
                    Log.d("MonitorRoom", "Firing alarm!");
                    Intent alarmIntent = new Intent(context, AlarmActivity.class);
                    alarmIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(alarmIntent);
                }
            }
        } catch (Exception e) {

        }
    }

    public void configure(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mRestClient = new RestClient(context.getAssets());
        mRestClient.configure(sharedPreferences);

        mAlarmActivated = sharedPreferences.getBoolean("temperature_alarm", false);
        if (mAlarmActivated) {
            // Get min and max temp
            mMaxTempThreshold = Integer.parseInt(sharedPreferences.getString("alarm_max_value", null));
            mMinTempThreshold = Integer.parseInt(sharedPreferences.getString("alarm_min_value", null));
            Log.d("MonitorRoom", "MaxTemp : " + mMaxTempThreshold.toString() + " MinTemp : " + mMinTempThreshold.toString());
        }

        // Update configuration map
        mRoomConfigMap = RoomConfig.GetMap(context);

    }

}
