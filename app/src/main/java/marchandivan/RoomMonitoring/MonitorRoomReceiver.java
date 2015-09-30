package marchandivan.RoomMonitoring;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;

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
    static HashMap<String, Float> mTemperatureMap = new HashMap<String, Float>();
    static HashMap<String, Float> mHumidityMap = new HashMap<String, Float>();

    // Rest client instance, used to access remote server
    private RestClient mRestClient;

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
    }

    // Deactivate the monitoring
    public static void Deactivate(Context context) {
        Intent intent = new Intent(context, MonitorRoomReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pendingIntent);
        Log.d("MonitorRoom", "Deactivate monitoring");
    }

    protected class Update extends AsyncTask<Void, Void, JSONArray> {

        protected JSONArray doInBackground(Void... voids) {
            return mRestClient.getArray("/api/v1/get/rooms");
        }

        protected void onPostExecute(JSONArray result) {
            try {
                for (int i = 0 ; i < result.length() ; i++) {
                    JSONObject room = result.getJSONObject(i);
                    if (room.has("temperature")) {
                        mTemperatureMap.put(room.getString("room"), Float.parseFloat(room.getString("temperature")));
                    }
                    if (room.has("humidity")) {
                        mHumidityMap.put(room.getString("room"), Float.parseFloat(room.getString("humidity")));
                    }
                }
            } catch (Exception e) {
                Log.d("UpdateRoom", "Error " + e.toString());
            }
        }
    }

    public MonitorRoomReceiver() {
    }

    static public HashMap<String, Float> GetTemperatures() {
        return mTemperatureMap;
    }

    static public HashMap<String, Float> GetHumidities() {
        return mHumidityMap;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("MonitorRoom", "notification received!");
        this.configure(context);

        // Update Room Temperature
        new Update().execute();

        // Check min/max temp
        boolean inQuietPeriod = mLastAlarmTimeStamp + mQuietPeriod > SystemClock.elapsedRealtime();
        if (!inQuietPeriod) {
            Float noahTemperature = mTemperatureMap.containsKey("noah") ? mTemperatureMap.get("noah") : null;
            if (mAlarmActivated && noahTemperature != null && (mMaxTempThreshold != null && noahTemperature > mMaxTempThreshold || mMinTempThreshold != null && noahTemperature < mMinTempThreshold)) {
                mLastAlarmTimeStamp = SystemClock.elapsedRealtime();
                Log.d("MonitorRoom", "Firing alarm!");
                Intent alarmIntent = new Intent(context, AlarmActivity.class);
                alarmIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(alarmIntent);
            }
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

    }

}
