package marchandivan.RoomMonitoring;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import marchandivan.RoomMonitoring.adapter.ThermostatModeAdapter;
import marchandivan.RoomMonitoring.db.AlarmConfig;
import marchandivan.RoomMonitoring.db.DeviceConfig;
import marchandivan.RoomMonitoring.db.SensorConfig;
import marchandivan.RoomMonitoring.dialog.AlarmDialogBuilder;
import marchandivan.RoomMonitoring.fragment.AlarmDisplayFragment;
import marchandivan.RoomMonitoring.receiver.MonitorRoomReceiver;
import marchandivan.RoomMonitoring.sensor.Sensor;
import marchandivan.RoomMonitoring.sensor.SensorFactory;

public class SensorDetailsActivity extends AppCompatActivity {
    private SensorConfig mSensorConfig;
    private DeviceConfig mDeviceConfig;
    private Sensor mSensor;
    private Boolean mAlarmActivated = false;
    private Handler mHandler;

    private void updateThermostatDisplay() {

        // Temperature
        TextView temperatureCommandView = (TextView) findViewById(R.id.temperature_command);
        Integer temperatureCommand = mSensorConfig.getThermostatTemperature();
        temperatureCommandView.setText(temperatureCommand == null ? getBaseContext().getString(R.string.temperature_command_place_holder) : String.format("%d\u00b0", temperatureCommand));

        // Mode
        Spinner thermostatModeSpinner = (Spinner) findViewById(R.id.thermostat_mode_spinner);
        ThermostatModeAdapter adapter = (ThermostatModeAdapter) thermostatModeSpinner.getAdapter();
        SensorConfig.ThermostatMode mode = mSensorConfig.getThermostatMode();
        int position = adapter == null ? 0 : adapter.getPosition(mode == null ? SensorConfig.ThermostatMode.OFF : mode);
        thermostatModeSpinner.setTag(R.id.position, position);
        thermostatModeSpinner.setSelection(position);
    }

    private Runnable mUpdateThermostatDisplayTask = new Runnable() {
        @Override
        public void run() {
            updateThermostatDisplay();
        }
    };

    private void updateDisplay() {
        try {
            // Thermostat
            if (mSensorConfig.getType() == SensorConfig.Type.THERMOSTAT) {
                updateThermostatDisplay();
            }

            // Open fragment transaction
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            // Alarm settings
            LinearLayout alarmDisplay = (LinearLayout)findViewById(R.id.alarm_display);
            if (mAlarmActivated) {
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
            } else {
                alarmDisplay.setAlpha(0.2f);
                //alarmDisplay.setVisibility(View.INVISIBLE);
            }

            // Something to commit?
            if (!fragmentTransaction.isEmpty()) {
                fragmentTransaction.commit();
            }
        } catch (Exception e) {
            Log.d("DeviceDetails", "Error " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_details);

        mHandler = new Handler();
        // Get sensor name
        Bundle args = getIntent().getExtras();
        mSensorConfig = new SensorConfig(getBaseContext(), args.getLong("sensor_id"));
        mSensorConfig.read();
        mDeviceConfig = new DeviceConfig(getBaseContext(), mSensorConfig.getDeviceId());
        mDeviceConfig.read();
        mSensor = SensorFactory.Get(mDeviceConfig.getType());

        // Set sensor name
        setTitle(mSensorConfig.getName());
        TextView sensorName = (TextView) findViewById(R.id.sensor_name);
        sensorName.setText(mSensorConfig.getName());

        // Icon
        ImageView icon = (ImageView) findViewById(R.id.sensor_header_icon);
        switch (mSensorConfig.getType()) {
            case THERMOSTAT:
                icon.setImageResource(R.drawable.thermostat);
                break;
            case THERMOMETER:
            default:
                icon.setImageResource(R.drawable.thermometer);
                break;
        }

        // Thermostat display
        final Spinner thermostatModeSpinner = (Spinner) findViewById(R.id.thermostat_mode_spinner);
        if (mSensorConfig.getType() == SensorConfig.Type.THERMOSTAT) {
            // Thermostat spinner
            final ThermostatModeAdapter adapter = new ThermostatModeAdapter(getBaseContext());
            thermostatModeSpinner.setAdapter(adapter);
            thermostatModeSpinner.setTag(R.id.position, 0);
            thermostatModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Integer tag = (Integer) thermostatModeSpinner.getTag(R.id.position);
                    if (tag != position) {
                        SensorConfig.ThermostatMode mode = (SensorConfig.ThermostatMode) adapter.getItem(position);
                        Integer temperatureCommand = mode == SensorConfig.ThermostatMode.OFF ? null : mSensorConfig.getThermostatTemperature();
                        mSensor.setThermostat(getBaseContext(), mDeviceConfig, mSensorConfig, mode, null, mUpdateThermostatDisplayTask);
                        view.setTag(R.id.position, position);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            // +/- buttons
            ImageButton tempPlus = (ImageButton) findViewById(R.id.temp_plus);
            tempPlus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Update
                    final Integer temperatureCommand = mSensorConfig.getThermostatTemperature();
                    if (temperatureCommand != null && temperatureCommand < 80) {
                        mSensorConfig.setThermostatTemperature(temperatureCommand + 1);
                        setThermostat();
                    }
                }
            });
            ImageButton tempMinus = (ImageButton) findViewById(R.id.temp_minus);
            tempMinus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Integer temperatureCommand = mSensorConfig.getThermostatTemperature();
                    if (temperatureCommand != null && temperatureCommand > 60) {
                        mSensorConfig.setThermostatTemperature(temperatureCommand - 1);
                        setThermostat();
                    }
                }
            });
        } else {
            thermostatModeSpinner.setVisibility(View.INVISIBLE);
            LinearLayout temperatureCommand = (LinearLayout) findViewById(R.id.temperature_command_display);
            temperatureCommand.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Temperature alarm activated
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mAlarmActivated = sharedPreferences.getBoolean("temperature_alarm", false);

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(marchandivan.RoomMonitoring.R.menu.menu_sensor, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.edit_sensor_name) {
            editSensorName();
            return true;
        } else if (id == R.id.remove_sensor) {
            removeSensor();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setThermostat() {
        updateDisplay();
        Runnable updateThermostatTask = new Runnable() {
            @Override
            public void run() {
                mSensor.setThermostat(getBaseContext(), mDeviceConfig, mSensorConfig, mSensorConfig.getThermostatMode(), mSensorConfig.getThermostatTemperature(), mUpdateThermostatDisplayTask);
            }
        };
        mHandler.removeCallbacksAndMessages(null);
        mHandler.postDelayed(updateThermostatTask, 5 * 1000);
    }

    private void editSensorName() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getBaseContext().getString(R.string.edit_sensor_name));

        // Set up the input
        final EditText input = new EditText(this);

        // Specify the type of input expected
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(mSensorConfig.getName());
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = input.getText().toString();
                if (!TextUtils.isEmpty(name)) {
                    // Set sensor name
                    mSensorConfig.updateName(name);
                    setTitle(mSensorConfig.getName());
                    TextView sensorName = (TextView) findViewById(R.id.sensor_name);
                    sensorName.setText(mSensorConfig.getName());
                }
            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private Activity getActivity() {
        return this;
    }

    private void removeSensor() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getBaseContext().getString(R.string.remove_sensor_confirm, mSensorConfig.getName()));

        // Set up the buttons
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Remove sensor
                mSensorConfig.delete();
                // Remove device... if no more sensor is linked to it
                List<SensorConfig> sensorConfigs = SensorConfig.GetList(getBaseContext(), mSensorConfig.getDeviceId());
                if (sensorConfigs.isEmpty()) {
                    mDeviceConfig.delete();
                }
                NavUtils.navigateUpFromSameTask(getActivity());
            }
        });

        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public void addAlarm(final View view) {
        if (mAlarmActivated) {
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
    }

    private void registerViews() {
        // Temperature
        TextView temperatureView = (TextView)this.findViewById(R.id.temperature);
        // Humidity
        TextView humidityView = (TextView)this.findViewById(R.id.humidity);
        MonitorRoomReceiver.Register(mSensorConfig.getId(), "detailed_view", temperatureView, humidityView);

        // Thermostat
        if (mSensorConfig.getType() == SensorConfig.Type.THERMOSTAT) {
            TextView temperatureCommandView = (TextView) findViewById(R.id.temperature_command);
            Spinner thermostatModeSpinner = (Spinner) findViewById(R.id.thermostat_mode_spinner);

            MonitorRoomReceiver.Register(mSensorConfig.getId(), "detailed_view", temperatureCommandView, thermostatModeSpinner);
        }
    }

    private void unRegisterViews() {
        MonitorRoomReceiver.Unregister(mSensorConfig.getId(), "detailed_view");
    }

}
