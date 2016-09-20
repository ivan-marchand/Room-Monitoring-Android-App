package marchandivan.RoomMonitoring.receiver;

import android.app.Activity;
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
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import marchandivan.RoomMonitoring.AlarmActivity;
import marchandivan.RoomMonitoring.R;
import marchandivan.RoomMonitoring.db.AlarmConfig;
import marchandivan.RoomMonitoring.db.DeviceConfig;
import marchandivan.RoomMonitoring.db.SensorConfig;
import marchandivan.RoomMonitoring.sensor.Sensor;
import marchandivan.RoomMonitoring.sensor.SensorFactory;

public class MonitorRoomReceiver extends BroadcastReceiver {

    // Polling interval (in ms)
    static final int mPollingInterval = 60 * 1000; // Every minute

    // Quiet period during which the alarm won't be fired after an alert
    static final long mQuietPeriod = 30 * 60 * 1000; // 30 minutes

    // true if alarm in activated
    private boolean mAlarmActivated = false;

    // TextView to update
    static private HashMap<Long, HashMap<String, Pair<TextView, TextView> > > mTextViewListeners = new HashMap<Long, HashMap<String, Pair<TextView,TextView>>>();

    // Callbacks
    static private HashMap<String, Runnable> mPreUpdateCallbacks = new HashMap<String, Runnable>();
    static private HashMap<String, Runnable> mPostUpdateCallbacks = new HashMap<String, Runnable>();

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

    protected class Update extends AsyncTask<Void, Void, JSONObject> {
        Context mContext;
        SensorConfig mSensorConfig;

        public Update(Context context, SensorConfig sensorConfig) {
            mContext = context;
            mSensorConfig = sensorConfig;
        }

        protected JSONObject doInBackground(Void... voids) {
            // Call callbacks
            for (Runnable callback : mPreUpdateCallbacks.values()) {
                callback.run();
            }
            DeviceConfig deviceConfig = new DeviceConfig(mContext, mSensorConfig.getDeviceId());
            deviceConfig.read();
            Sensor sensor = SensorFactory.Get(deviceConfig.getType());
            return sensor.getSensorMeasure(mContext, mSensorConfig);
        }

        protected void onPostExecute(JSONObject result) {
            try {
                long lastUpdate = System.currentTimeMillis();
                mSensorConfig.update(lastUpdate, result);

                // Display error if any
                if (result.has("error")) {
                    Toast.makeText(mContext, result.getString("error"), Toast.LENGTH_SHORT).show();
                }
                // Update registered views
                UpdateViews(mSensorConfig.getId(), mSensorConfig.getTemperature(), mSensorConfig.getHumidity());

                // Call callbacks
                for (Runnable callback : mPostUpdateCallbacks.values()) {
                    callback.run();
                }

            } catch (Exception e) {
                Log.d("UpdateRoom", "Error " + e.toString());
            }
            // Check alarm
            checkAlarm(mContext, mSensorConfig);
        }
    }

    // Update room temp
    public static boolean Update(Activity parent) {
        MonitorRoomReceiver monitorRoomReceiver = new MonitorRoomReceiver();
        return monitorRoomReceiver.update(parent);
    }

    public boolean update(Activity parent) {

        this.configure(parent.getBaseContext());
        final Context context = parent.getBaseContext();

        // Update Room Temperature
        ArrayList<Update> updates = new ArrayList<>();
        for (SensorConfig sensorConfig: SensorConfig.GetMap(context).values()) {
            Update update = new Update(context, sensorConfig);
            updates.add(update);
            update.execute();
        }

        // Wait for all tasks to complete
        for (Update update: updates) {
            try {
                update.get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                parent.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, context.getString(R.string.timeout), Toast.LENGTH_LONG).show();
                    }
                });
                return false;
            }
        }
        return true;
    }

    public static void UpdateViews(Context context) {
        for (SensorConfig sensorConfig: SensorConfig.GetMap(context).values()) {
            UpdateViews(sensorConfig.getId(), sensorConfig.getTemperature(), sensorConfig.getHumidity());
        }
    }

    private static void UpdateViews(long sensorId, Double temperature, Double humidity) {
        if (mTextViewListeners.containsKey(sensorId)) {
            for(Pair<TextView, TextView> sensorViews : mTextViewListeners.get(sensorId).values()) {
                Log.d("MonitorRoom", "Updating sensor " + sensorId);
                Context context = sensorViews.first.getContext();
                // Temperature
                sensorViews.first.setText(temperature != null ? String.format("%.1f F", temperature) : context.getString(R.string.temperature_place_holder));
                // Humidity
                sensorViews.second.setText(humidity != null ? String.format("%.1f %%", humidity) : context.getString(R.string.humidity_place_holder));
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        this.configure(context);

        // Update Room Temperature
        for (SensorConfig sensorConfig: SensorConfig.GetMap(context).values()) {
            new Update(context, sensorConfig).execute();
        }

    }

    private void checkAlarm(Context context, SensorConfig sensorConfig) {
        try {
            // Check min/max temp
            AlarmConfig alarmConfig = new AlarmConfig(context, sensorConfig.getId());
            if (mAlarmActivated && !sensorConfig.measureHasExpired() && !inQuietPeriod(sensorConfig)) {
                for (AlarmConfig.Alarm alarm : alarmConfig.read()) {
                    if (alarm.mAlarmActive && exceedThreshold(sensorConfig, alarm)) {
                        sensorConfig.updateAlarm();
                        Log.d("MonitorRoom", "Firing alarm!");
                        Intent alarmIntent = new Intent(context, AlarmActivity.class);
                        Bundle args = new Bundle();
                        // Setup the alarm
                        alarmIntent.putExtra("sensor_id", sensorConfig.getId());
                        alarmIntent.putExtra("max_temperature", alarm.mMaxTemp);
                        alarmIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(alarmIntent);

                        // No need to start alarm several time
                        return;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean inQuietPeriod(SensorConfig sensorConfig) {
        return sensorConfig.getLastAlarm() + mQuietPeriod > System.currentTimeMillis();
    }

    private boolean exceedThreshold(SensorConfig sensorConfig, AlarmConfig.Alarm alarm) throws JSONException {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        Integer nowMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
        Double temperature = sensorConfig.getTemperature();
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

        mAlarmActivated = sharedPreferences.getBoolean("temperature_alarm", false);

    }


    static public void Register(long sensorId, String key, TextView tempTextView, TextView humidityTextView) {
        try {

            if (!mTextViewListeners.containsKey(sensorId)) {
                mTextViewListeners.put(sensorId, new HashMap<String, Pair<TextView, TextView>>());
            }
            mTextViewListeners.get(sensorId).put(key, new Pair<TextView, TextView>(tempTextView, humidityTextView));

            // Update display
            SensorConfig sensorConfig = new SensorConfig(tempTextView.getContext(), sensorId);
            sensorConfig.read();
            UpdateViews(sensorId, sensorConfig.getTemperature(), sensorConfig.getHumidity());

        }
        catch (Exception e) {

        }
    }

    static public void Unregister(long sensorId, String key) {
        if (mTextViewListeners.containsKey(sensorId) &&  mTextViewListeners.get(sensorId).containsKey(key)) {
            mTextViewListeners.get(sensorId).remove(key);
        }
    }

    static public void AddPostUpdateCallback(long sensorId, String key, Runnable callback) {
        mPostUpdateCallbacks.put(sensorId + "_" + key, callback);
    }

    static public void RemovePostUpdateCallback(long sensorId, String key) {
        if (mPostUpdateCallbacks.containsKey(sensorId + "_" + key)) {
            mPostUpdateCallbacks.remove(sensorId  + "_" + key);
        }
    }

    static public void AddPreUpdateCallback(long sensorId, String key, Runnable callback) {
        mPreUpdateCallbacks.put(sensorId + "_" + key, callback);
    }

    static public void RemovePreUpdateCallback(long sensorId, String key) {
        if (mPreUpdateCallbacks.containsKey(sensorId + "_" + key)) {
            mPreUpdateCallbacks.remove(sensorId  + "_" + key);
        }
    }

}
