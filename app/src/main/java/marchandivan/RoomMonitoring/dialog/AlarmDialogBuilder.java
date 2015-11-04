package marchandivan.RoomMonitoring.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.ArrayList;

import marchandivan.RoomMonitoring.R;
import marchandivan.RoomMonitoring.db.AlarmConfig;
import marchandivan.RoomMonitoring.db.RoomConfig;

/**
 * Created by ivan on 11/2/15.
 */
public class AlarmDialogBuilder {
    private Context mContext;
    private LayoutInflater mInflater;
    private AlertDialog.Builder mBuilder;
    private Runnable mPostSaveCallback = null;

    public AlarmDialogBuilder(Context context, LayoutInflater inflater) {
        mContext = context;
        mInflater = inflater;
        mBuilder = new AlertDialog.Builder(context);
    }

    // Set callback to be called after save (for display update)
    public void setPostSaveCallback(Runnable runnable) {
        mPostSaveCallback = runnable;
    }

    // Add new alarm
    public AlertDialog create(String room) {
        // Build the dialog
        mBuilder.setTitle("Add Alarm");
        return createDialog(room, new AlarmConfig(mContext, room).getAlarmInstance());
    }

    // Edit alarm
    public AlertDialog edit(String room, AlarmConfig.Alarm alarm) {
        // Build the dialog
        mBuilder.setTitle("Edit Alarm");
        return createDialog(room, alarm);
    }

    private AlertDialog createDialog(String room, final AlarmConfig.Alarm alarm) {
        // Inflate layout
        final View dialogView = mInflater.inflate(R.layout.add_alarm_dialog, null);

        // Get Room and alarm config
        final AlarmConfig alarmConfig = new AlarmConfig(mContext, room);
        final RoomConfig roomConfig = new RoomConfig(mContext, room);

        // Set time pickers format
        TimePicker startTime = (TimePicker) dialogView.findViewById(R.id.alarm_start_time);
        startTime.setIs24HourView(true);
        TimePicker stopTime = (TimePicker) dialogView.findViewById(R.id.alarm_stop_time);
        stopTime.setIs24HourView(true);

        // Alarm already exists ?
        Switch activeAnytimeSwitch = (Switch) dialogView.findViewById(R.id.alarm_active_anytime);
        final LinearLayout timeSettings = (LinearLayout) dialogView.findViewById(R.id.alarm_time_settings);
        if(roomConfig.read() && alarm.mId != 0)
        {
            // Alarm On/Off
            Switch alarmOnOff = (Switch) dialogView.findViewById(R.id.alarm_on_off);
            alarmOnOff.setChecked(alarm.mAlarmActive);

            // Min/Max temp
            TextView minTemp = (TextView) dialogView.findViewById(R.id.alarm_min_temp);
            minTemp.setText(String.valueOf(alarm.mMinTemp));
            TextView maxTemp = (TextView) dialogView.findViewById(R.id.alarm_max_temp);
            maxTemp.setText(String.valueOf(alarm.mMaxTemp));

            // Active any time?
            Boolean activeAnyTime = alarm.isActiveAnyTime();
            activeAnytimeSwitch.setChecked(activeAnyTime);
            timeSettings.setVisibility(activeAnyTime ? View.GONE : View.VISIBLE);

            // Start/Stop time
            startTime.setCurrentHour(alarm.mStartTime.first);
            startTime.setCurrentMinute(alarm.mStartTime.second);
            stopTime.setCurrentHour(alarm.mStopTime.first);
            stopTime.setCurrentMinute(alarm.mStopTime.second);
        }

        // Setup click listener to update time setting visibility
        activeAnytimeSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Switch activeAnytimeSwitch = (Switch) v;
                timeSettings.setVisibility(activeAnytimeSwitch.isChecked() ? View.GONE : View.VISIBLE);
            }
        });

        mBuilder.setView(dialogView);

        // Add confirm/cancel buttons
        mBuilder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        // Alarm On/Off
                        Switch alarmOnOff = (Switch) dialogView.findViewById(R.id.alarm_on_off);
                        alarm.mAlarmActive = alarmOnOff.isChecked();

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
                        alarmConfig.update(alarm);

                        // Call callback if any
                        if (mPostSaveCallback != null) {
                            mPostSaveCallback.run();
                        }
                    }
                }

        );

        // Add confirm/cancel buttons
        mBuilder.setNegativeButton("Cancel",null);

        return mBuilder.create();
    }


}
