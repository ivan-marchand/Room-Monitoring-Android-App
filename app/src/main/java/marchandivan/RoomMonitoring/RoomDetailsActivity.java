package marchandivan.RoomMonitoring;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.ArrayList;

import marchandivan.RoomMonitoring.db.AlarmConfig;
import marchandivan.RoomMonitoring.db.SensorConfig;
import marchandivan.RoomMonitoring.dialog.AlarmDialogBuilder;
import marchandivan.RoomMonitoring.fragment.AlarmDisplayFragment;
import marchandivan.RoomMonitoring.fragment.DeviceFragment;
import marchandivan.RoomMonitoring.receiver.MonitorRoomReceiver;

public class RoomDetailsActivity extends AppCompatActivity {
    private SensorConfig mSensorConfig;
    private Boolean mAlarmActived = false;

    private void updateDisplay() {
        try {
            // Open fragment transaction
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            // Update temperature, humidity display
            JSONObject data = mSensorConfig.getData();
            Log.d("SensorDetailsActivity", data.toString());

            // Display device
            /*JSONArray devices = new JSONArray();
            if (data.has("devices")) {
                devices = data.getJSONArray("devices");
            }

            // Anything to display ?
            LinearLayout deviceDisplay = (LinearLayout)this.findViewById(R.id.device_display);
            deviceDisplay.setVisibility(devices.length() == 0 ? View.GONE : View.VISIBLE);

            // Loop on each device
            for (int i = 0 ; i < devices.length() ; i++ ) {
                JSONObject device = devices.getJSONObject(i);
                String deviceType = device.getString("type");
                String deviceName = device.getString("name");
                DeviceFragment deviceFragment = DeviceFragment.newInstance(deviceType,
                        deviceName,
                        device.getJSONArray("commands"));
                String aTag = "device_fragment_" + deviceName;
                if (fragmentManager.findFragmentByTag(aTag) != null) {
                    fragmentTransaction.replace(R.id.device_list, deviceFragment, aTag);
                } else {
                    fragmentTransaction.add(R.id.device_list, deviceFragment, aTag);
                }
            }*/

            // Alarm settings
            LinearLayout alarmDisplay = (LinearLayout)findViewById(R.id.alarm_display);
            if (mAlarmActived) {
                alarmDisplay.setVisibility(View.VISIBLE);
                SensorConfig sensorConfig = new SensorConfig(this, mSensorConfig.getId());
                if (sensorConfig.read()) {
                    AlarmConfig alarmConfig = new AlarmConfig(this, mSensorConfig.getId());
                    ArrayList<AlarmConfig.Alarm> alarms = alarmConfig.read();
                    LinearLayout alarmDisplayContainer = (LinearLayout) this.findViewById(R.id.alarm_list);
                    alarmDisplayContainer.removeAllViews();
                    int i = 0;
                    for (AlarmConfig.Alarm alarm : alarms) {
                        AlarmDisplayFragment alarmDisplayFragment = AlarmDisplayFragment.newInstance(mSensorConfig.getId(), alarm.getId(), i++ % 2 == 0);
                        // Add fragment to view
                        fragmentTransaction.add(R.id.alarm_list, alarmDisplayFragment);
                    }
                }
            }
            else {
                alarmDisplay.setVisibility(View.GONE);
            }
            // Something to commit?
            if (!fragmentTransaction.isEmpty()) {
                fragmentTransaction.commit();
            }
        } catch (Exception e) {
            Log.d("DeviceDetails", "Error " + e.getMessage());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_details);

        // Get sensor name
        Bundle args = getIntent().getExtras();
        mSensorConfig = new SensorConfig(getBaseContext(), args.getLong("sensor_id"));
        mSensorConfig.read();

        // Set sensor name, capitalized (First letter upper case)
        TextView sensorName = (TextView)this.findViewById(R.id.sensor_name);
        sensorName.setText(mSensorConfig.getName());

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Temperature alarm activated
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mAlarmActived = sharedPreferences.getBoolean("temperature_alarm", false);

        // Update the display
        updateDisplay();

        // Register temp/humidity views
        registerViews();
    }

    @Override
    protected void onDestroy() {
        unRegisterViews();
        super.onDestroy();
    }

    public void addAlarm(final View view) {
        AlarmDialogBuilder builder = new AlarmDialogBuilder(this);

        // Set post save callback to update display
        builder.setPostSaveCallback(new Runnable() {
            @Override
            public void run() {
                // Update display
                updateDisplay();
            }
        });

        // Show the dialog
        AlertDialog dialog = builder.create(mSensorConfig.getId());
        dialog.show();

    }

    private void registerViews() {
        // Temperature
        TextView temperatureView = (TextView)this.findViewById(R.id.temperature);
        // Humidity
        TextView humidityView = (TextView)this.findViewById(R.id.humidity);
        MonitorRoomReceiver.Register(mSensorConfig.getId(), "detailed_view", temperatureView, humidityView);
    }

    private void unRegisterViews() {
        MonitorRoomReceiver.Unregister(mSensorConfig.getId(), "detailed_view");
    }

}
