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

import marchandivan.RoomMonitoring.R;
import marchandivan.RoomMonitoring.db.AlarmConfig;
import marchandivan.RoomMonitoring.db.SensorConfig;

/**
 * Created by ivan on 11/2/15.
 */
public class AlarmDialogBuilder {
    private Context mContext;
    private AlertDialog.Builder mBuilder;
    private Runnable mPostSaveCallback = null;

    public AlarmDialogBuilder(Context context) {
        mContext = context;
        mBuilder = new AlertDialog.Builder(context);
    }

    // Set callback to be called after save (for display update)
    public void setPostSaveCallback(Runnable runnable) {
        mPostSaveCallback = runnable;
    }

    // Add new alarm
    public AlertDialog create(long sensor) {
        // Build the dialog
        mBuilder.setTitle("Add Alarm");
        return createDialog(sensor, new AlarmConfig(mContext, sensor).getAlarmInstance());
    }

    // Edit alarm
    public AlertDialog edit(long sensor, AlarmConfig.Alarm alarm) {
        // Build the dialog
        mBuilder.setTitle("Edit Alarm");
        return createDialog(sensor, alarm);
    }

    private AlertDialog createDialog(long sensor, final AlarmConfig.Alarm alarm) {
        // Inflate layout
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View dialogView = inflater.inflate(R.layout.add_alarm_dialog, null);

        // Get Room and alarm config
        final AlarmConfig alarmConfig = new AlarmConfig(mContext, sensor);
        final SensorConfig sensorConfig = new SensorConfig(mContext, sensor);

        // Alarm already exists ?
        Switch activeAnytimeSwitch = (Switch) dialogView.findViewById(R.id.alarm_active_anytime);
        final LinearLayout timeSettings = (LinearLayout) dialogView.findViewById(R.id.alarm_time_settings);
        if(sensorConfig.read() && alarm.getId() != 0)
        {
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
            TimePicker startTime = (TimePicker) dialogView.findViewById(R.id.alarm_start_time);
            TimePicker stopTime = (TimePicker) dialogView.findViewById(R.id.alarm_stop_time);
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

                        // Activate alarm
                        alarm.mAlarmActive = true;

                        // Min/Max temp
                        TextView minTemp = (TextView) dialogView.findViewById(R.id.alarm_min_temp);
                        try {
                            alarm.mMinTemp = Integer.parseInt(minTemp.getText().toString());
                        } catch (NumberFormatException e) {
                            Toast.makeText(dialogView.getContext(),
                                    dialogView.getContext().getString(R.string.temperature_value_missing_or_invalid, dialogView.getContext().getString(R.string.min)),
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        TextView maxTemp = (TextView) dialogView.findViewById(R.id.alarm_max_temp);
                        try {
                            alarm.mMaxTemp = Integer.parseInt(maxTemp.getText().toString());
                        } catch (NumberFormatException e) {
                            Toast.makeText(dialogView.getContext(),
                                    dialogView.getContext().getString(R.string.temperature_value_missing_or_invalid, dialogView.getContext().getString(R.string.max)),
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Check value consistency
                        if (alarm.mMaxTemp <= alarm.mMinTemp) {
                            Toast.makeText(dialogView.getContext(),
                                    dialogView.getContext().getString(R.string.max_temperature_must_be_higher_than_min_temperature),
                                    Toast.LENGTH_SHORT).show();
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
                                Toast.makeText(dialogView.getContext(),
                                        dialogView.getContext().getString(R.string.start_and_stop_time_has_to_be_different),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }

                        // Save result
                        alarmConfig.update(alarm);

                        // Call callback if any
                        if (mPostSaveCallback != null) {
                            mPostSaveCallback.run();
                        }

                        // Alarm
                        Toast.makeText(dialogView.getContext(),
                                dialogView.getContext().getString(R.string.alarm_is_onoff, dialogView.getContext().getString(R.string.on)),
                                Toast.LENGTH_SHORT).show();
                    }
                }

        );

        // Add confirm/cancel buttons
        mBuilder.setNegativeButton("Cancel",null);

        return mBuilder.create();
    }


}
