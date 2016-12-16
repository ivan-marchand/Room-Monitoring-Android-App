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
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import marchandivan.RoomMonitoring.AlarmActivity;
import marchandivan.RoomMonitoring.R;
import marchandivan.RoomMonitoring.adapter.ThermostatModeAdapter;
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
    static private HashMap<Long, HashMap<String, Pair<TextView, TextView> > > mTemperatureDisplayListeners = new HashMap<>();
    static private HashMap<Long, HashMap<String, Pair<TextView, Spinner> > > mThermostatDisplayListeners = new HashMap<>();

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
        Log.d("MonitorSensor", "Deactivate monitoring");

        // Disable boot receiver
        ComponentName receiver = new ComponentName(context, BootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    protected class Update extends AsyncTask<Void, Void, Void> {
        Context mContext;
        DeviceConfig mDeviceConfig;
        List<SensorConfig> mSensorConfigs;

        public Update(Context context, DeviceConfig deviceConfig) {
            mContext = context;
            mDeviceConfig = deviceConfig;
            mSensorConfigs = SensorConfig.GetList(context, mDeviceConfig.getId());
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Log.d("MonitorSensor", "Update device " + mDeviceConfig.toString());
            Sensor sensor = SensorFactory.Get(mDeviceConfig.getType());
            sensor.updateSensor(mContext, mDeviceConfig, mSensorConfigs);
            return null;
        }

        @Override
        protected void onPostExecute(Void voids) {
            for (SensorConfig sensorConfig : mSensorConfigs) {
                if (sensorConfig.isVisible()) {
                    // Update registered views
                    UpdateViews(sensorConfig);

                    // Check alarm
                    checkAlarm(mContext, sensorConfig);
                }
            }
        }
    }

    // Update sensor temp
    public static void Update(Activity parent) {
        MonitorRoomReceiver monitorRoomReceiver = new MonitorRoomReceiver();
        monitorRoomReceiver.update(parent);
    }

    public void update(Activity parent) {

        this.configure(parent.getBaseContext());
        final Context context = parent.getBaseContext();

        // Update Room Temperature
        LinkedList<Update> updates = new LinkedList<>();
        for (DeviceConfig deviceConfig: DeviceConfig.GetMap(context).values()) {
            Update update = new Update(context, deviceConfig);
            updates.add(update);
            // Execute in parallel
            update.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        // Wait for all tasks to complete
        for (Update update: updates) {
            try {
                update.get(30, TimeUnit.SECONDS);
            } catch (Exception e) {
                parent.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, context.getString(R.string.timeout), Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
    }

    public static void UpdateViews(Context context) {
        for (SensorConfig sensorConfig: SensorConfig.GetMap(context).values()) {
            UpdateViews(sensorConfig);
        }
    }

    private static void UpdateViews(SensorConfig sensorConfig) {
        UpdateViews(sensorConfig.getId(), sensorConfig.getTemperature(), sensorConfig.getHumidity(), sensorConfig.getThermostatTemperature(), sensorConfig.getThermostatMode());
    }

    private static void UpdateViews(long sensorId, Double temperature, Double humidity, Integer temperatureCommand, SensorConfig.ThermostatMode mode) {
        if (mTemperatureDisplayListeners.containsKey(sensorId)) {
            Log.d("MonitorSensor", "Update thermometer views for sensor " + sensorId);
            for(Pair<TextView, TextView> sensorViews : mTemperatureDisplayListeners.get(sensorId).values()) {
                Context context = sensorViews.first.getContext();
                // Temperature
                sensorViews.first.setText(temperature != null ? String.format("%.1f\u00b0", temperature) : context.getString(R.string.temperature_place_holder));
                // Humidity
                sensorViews.second.setText(humidity != null ? String.format("%.1f %%", humidity) : context.getString(R.string.humidity_place_holder));
            }
        } else {
            Log.d("MonitorSensor", "No view registered for sensor " + sensorId);
        }

        if (mThermostatDisplayListeners.containsKey(sensorId)) {
            Log.d("MonitorSensor", "Update thermostat views for sensor " + sensorId);
            for(Pair<TextView, Spinner> sensorViews : mThermostatDisplayListeners.get(sensorId).values()) {
                Context context = sensorViews.first.getContext();
                // Temperature
                sensorViews.first.setText(temperatureCommand != null ? String.format("%1d\u00b0", temperatureCommand) : context.getString(R.string.temperature_command_place_holder));
                // Mode
                Spinner thermostatModeSpinner = sensorViews.second;
                ThermostatModeAdapter adapter = (ThermostatModeAdapter) thermostatModeSpinner.getAdapter();
                int position = adapter == null ? 0 : adapter.getPosition(mode == null ? SensorConfig.ThermostatMode.OFF : mode);
                thermostatModeSpinner.setTag(R.id.position, position);
                thermostatModeSpinner.setSelection(position);
            }
        } else {
            Log.d("MonitorSensor", "No view registered for sensor " + sensorId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        this.configure(context);

        // Update Room Temperature
        for (DeviceConfig deviceConfig: DeviceConfig.GetMap(context).values()) {
            new Update(context, deviceConfig).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

    }

    private void checkAlarm(Context context, SensorConfig sensorConfig) {
        try {
            // Read from Db in case a current task already fired an alarm
            sensorConfig.read();

            // Check min/max temp
            AlarmConfig alarmConfig = new AlarmConfig(context, sensorConfig.getId());
            if (mAlarmActivated && !sensorConfig.measureHasExpired() && !inQuietPeriod(sensorConfig)) {
                for (AlarmConfig.Alarm alarm : alarmConfig.read()) {
                    if (alarm.mAlarmActive && exceedThreshold(sensorConfig, alarm)) {
                        sensorConfig.updateAlarm();
                        Log.d("MonitorRoom", "Firing alarm!");
                        Intent alarmIntent = new Intent(context, AlarmActivity.class);

                        // Setup the alarm
                        alarmIntent.putExtra("sensor_id", sensorConfig.getId());
                        alarmIntent.putExtra("temperature", sensorConfig.getTemperature());
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

            if (!mTemperatureDisplayListeners.containsKey(sensorId)) {
                mTemperatureDisplayListeners.put(sensorId, new HashMap<String, Pair<TextView, TextView>>());
            }
            mTemperatureDisplayListeners.get(sensorId).put(key, new Pair<TextView, TextView>(tempTextView, humidityTextView));

            // Update display
            SensorConfig sensorConfig = new SensorConfig(tempTextView.getContext(), sensorId);
            sensorConfig.read();
            UpdateViews(sensorConfig);

        }
        catch (Exception e) {

        }
    }

    static public void Register(long sensorId, String key, TextView tempTextView, Spinner modeSpinner) {
        try {

            if (!mThermostatDisplayListeners.containsKey(sensorId)) {
                mThermostatDisplayListeners.put(sensorId, new HashMap<String, Pair<TextView, Spinner>>());
            }
            mThermostatDisplayListeners.get(sensorId).put(key, new Pair<TextView, Spinner>(tempTextView, modeSpinner));

            // Update display
            SensorConfig sensorConfig = new SensorConfig(tempTextView.getContext(), sensorId);
            sensorConfig.read();
            UpdateViews(sensorConfig);

        }
        catch (Exception e) {

        }
    }

    static public void Unregister(long sensorId, String key) {
        if (mTemperatureDisplayListeners.containsKey(sensorId) && mTemperatureDisplayListeners.get(sensorId).containsKey(key)) {
            mTemperatureDisplayListeners.get(sensorId).remove(key);
        }
        if (mThermostatDisplayListeners.containsKey(sensorId) && mThermostatDisplayListeners.get(sensorId).containsKey(key)) {
            mThermostatDisplayListeners.get(sensorId).remove(key);
        }
    }

}
