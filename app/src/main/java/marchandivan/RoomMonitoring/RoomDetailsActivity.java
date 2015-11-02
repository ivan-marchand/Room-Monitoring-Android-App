package marchandivan.RoomMonitoring;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import marchandivan.RoomMonitoring.db.AlarmConfig;
import marchandivan.RoomMonitoring.db.RoomConfig;
import marchandivan.RoomMonitoring.receiver.MonitorRoomReceiver;

public class RoomDetailsActivity extends AppCompatActivity {
    private Handler mHandler;
    private String mRoom;

    // Display refresh rate (in ms)
    private int mDisplayRefreshInterval = 30 * 1000; // Every 30s

    private void updateDisplay() {
        try {
            // Update temperature, humidity display
            if (MonitorRoomReceiver.GetRooms().containsKey(mRoom)) {
                JSONObject room = MonitorRoomReceiver.GetRooms().get(mRoom);
                if (room.has("temperature")) {
                    // Temperature
                    TextView temperatureView = (TextView) this.findViewById(R.id.temperature);
                    temperatureView.setText(String.format("%.1f F", Float.parseFloat(room.getString("temperature"))));
                }
                if (room.has("humidity")) {
                    // Humidity
                    TextView humidityView = (TextView) this.findViewById(R.id.humidity);
                    humidityView.setText(String.format("%.1f %%", Float.parseFloat(room.getString("humidity"))));
                }

                // Display device
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                JSONArray devices = room.getJSONArray("devices");

                // Anythin to display ?
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
                }
                // Something to commit?
                if (!fragmentTransaction.isEmpty()) {
                    fragmentTransaction.commit();
                }
            }

            // Alarm settings
            RoomConfig roomConfig = new RoomConfig(this, mRoom);
            if (roomConfig.read()) {
                AlarmConfig alarmConfig = new AlarmConfig(this, mRoom);
                ArrayList<AlarmConfig.Alarm> alarms = alarmConfig.read();
                LinearLayout alarmDisplayContainer = (LinearLayout)this.findViewById(R.id.alarm_display);
                alarmDisplayContainer.removeAllViews();
                for (AlarmConfig.Alarm alarm : alarms) {
                    // Display temp range
                    TextView alarmTempRange = new TextView(this);
                    alarmTempRange.setTextSize(20);
                    alarmTempRange.setText(String.format("Min - Max (F) : %d - %d", alarm.mMinTemp, alarm.mMaxTemp));

                    // Display alarm on/off icon
                    ImageView alarmIcon = new ImageView(this);
                    alarmIcon.setImageResource(roomConfig.mAlarmActive ? R.drawable.alarm : R.drawable.alarm_off);

                    // Add views to container
                    alarmDisplayContainer.addView(alarmIcon);
                    alarmDisplayContainer.addView(alarmTempRange);

                    // Display time range
                    if (!alarm.isActiveAnyTime()) {
                        TextView alarmTimeRange = new TextView(this);
                        alarmTimeRange.setTextSize(20);
                        alarmTimeRange.setText(String.format("From %02d:%02d to %02d:%02d",
                                alarm.mStartTime.first, alarm.mStartTime.second, alarm.mStopTime.first, alarm.mStopTime.second));
                        alarmDisplayContainer.addView(alarmTimeRange);
                    }
                }

            }
        } catch (Exception e) {
            Log.d("DeviceDetails", "Error " + e.getMessage());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_details);

        // Init the callback handler
        mHandler = new Handler();

        // Get room name
        Bundle args = getIntent().getExtras();
        mRoom = args.getString("room");

        // Set room name, capitalized (First letter upper case)
        TextView roomName = (TextView)this.findViewById(R.id.room_name);
        char[] roomCapitalized = mRoom.toCharArray();
        roomCapitalized[0] = Character.toUpperCase(roomCapitalized[0]);
        roomName.setText(new String(roomCapitalized));

        // Update temperature, humidity
        updateDisplay();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Get current room temp
        MonitorRoomReceiver.Update(this);

        // Update the display
        updateDisplay();
    }

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_room_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }*/

    public void editAlarm(final View view) {
        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Alarm");

        // Inflate layout
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.add_alarm_dialog, null);
        final AlarmConfig alarmConfig = new AlarmConfig(this, mRoom);
        final ArrayList<AlarmConfig.Alarm> alarms = alarmConfig.read();
        final RoomConfig roomConfig = new RoomConfig(this, mRoom);

        // Set time pickers format
        TimePicker startTime = (TimePicker)dialogView.findViewById(R.id.alarm_start_time);
        startTime.setIs24HourView(true);
        TimePicker stopTime = (TimePicker)dialogView.findViewById(R.id.alarm_stop_time);
        stopTime.setIs24HourView(true);

        // Alarm already exists ?
        if (roomConfig.read() && !alarms.isEmpty()) {
            AlarmConfig.Alarm alarm = alarms.get(0);

            // Alarm On/Off
            Switch alarmOnOff = (Switch)dialogView.findViewById(R.id.alarm_on_off);
            alarmOnOff.setChecked(roomConfig.mAlarmActive);

            // Min/Max temp
            TextView minTemp = (TextView)dialogView.findViewById(R.id.alarm_min_temp);
            minTemp.setText(String.valueOf(alarm.mMinTemp));
            TextView maxTemp = (TextView)dialogView.findViewById(R.id.alarm_max_temp);
            maxTemp.setText(String.valueOf(alarm.mMaxTemp));

            // Active any time?
            Switch activeAnytimeSwitch = (Switch)dialogView.findViewById(R.id.alarm_active_anytime);
            Boolean activeAnyTime = alarm.isActiveAnyTime();
            activeAnytimeSwitch.setChecked(activeAnyTime);
            final LinearLayout timeSettings = (LinearLayout)dialogView.findViewById(R.id.alarm_time_settings);
            timeSettings.setVisibility(activeAnyTime ? View.GONE: View.VISIBLE);
            activeAnytimeSwitch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Switch activeAnytimeSwitch = (Switch)v;
                    timeSettings.setVisibility(activeAnytimeSwitch.isChecked() ? View.GONE : View.VISIBLE);
                }
            });

            // Start/Stop time
            startTime.setCurrentHour(alarm.mStartTime.first);
            startTime.setCurrentMinute(alarm.mStartTime.second);
            stopTime.setCurrentHour(alarm.mStopTime.first);
            stopTime.setCurrentMinute(alarm.mStopTime.second);
        }
        builder.setView(dialogView);

        // Add confirm/cancel buttons
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ArrayList<AlarmConfig.Alarm> alarms = alarmConfig.read();
                AlarmConfig.Alarm alarm = null;
                if (alarms.isEmpty()) {
                    // No existing alarm, create a new one
                    alarm = alarmConfig.getAlarmInstance();
                } else {
                    alarm = alarms.get(0);
                }

                // Alarm On/Off
                Switch alarmOnOff = (Switch) dialogView.findViewById(R.id.alarm_on_off);
                roomConfig.mAlarmActive = alarmOnOff.isChecked();

                // Min/Max temp
                TextView minTemp = (TextView) dialogView.findViewById(R.id.alarm_min_temp);
                try {
                    alarm.mMinTemp = Integer.parseInt(minTemp.getText().toString());
                } catch (NumberFormatException e) {
                    Toast.makeText(dialogView.getContext(), "Min Temperature value missing or invalid", Toast.LENGTH_SHORT).show();
                    return;
                }

                TextView maxTemp = (TextView) dialogView.findViewById(R.id.alarm_max_temp);
                try {
                    alarm.mMaxTemp = Integer.parseInt(maxTemp.getText().toString());
                } catch (NumberFormatException e) {
                    Toast.makeText(dialogView.getContext(), "Max Temperature value missing or invalid", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Check value consistency
                if (alarm.mMaxTemp <= alarm.mMinTemp) {
                    Toast.makeText(dialogView.getContext(), "Max Temperature value must be higher than Min temperature value", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Active anytime
                Switch activeAnytimeSwitch = (Switch) dialogView.findViewById(R.id.alarm_active_anytime);
                if (activeAnytimeSwitch.isChecked()) {
                    alarm.mStartTime = new Pair<Integer, Integer>(0, 0);
                    alarm.mStopTime = new Pair<Integer, Integer>(0, 0);
                } else {
                    // Start/Stop time
                    TimePicker startTime = (TimePicker) dialogView.findViewById(R.id.alarm_start_time);
                    TimePicker stopTime = (TimePicker) dialogView.findViewById(R.id.alarm_stop_time);
                    if (!startTime.getCurrentHour().equals(stopTime.getCurrentHour()) || !startTime.getCurrentMinute().equals(stopTime.getCurrentMinute())) {
                        alarm.mStartTime = new Pair<Integer, Integer>(startTime.getCurrentHour(), startTime.getCurrentMinute());
                        alarm.mStopTime = new Pair<Integer, Integer>(stopTime.getCurrentHour(), stopTime.getCurrentMinute());
                    } else {
                        Toast.makeText(dialogView.getContext(), "Start and Stop time has to be different", Toast.LENGTH_SHORT).show();
                    }
                }

                // Save result
                roomConfig.update();
                alarmConfig.update(alarm);

                // Update display
                updateDisplay();
            }
        });

        // Add confirm/cancel buttons
        builder.setNegativeButton("Cancel", null);

        // Show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private Runnable mDisplayRefresher = new Runnable() {
        @Override
        public void run() {
            try {
                if (hasWindowFocus()) {
                    updateDisplay();
                }
            } catch (Exception e) {

            } finally {
                mHandler.removeCallbacks(this);
                mHandler.postDelayed(this, MonitorRoomReceiver.GetRooms().isEmpty() ? 1000 : mDisplayRefreshInterval);
            }
        }
    };

}
