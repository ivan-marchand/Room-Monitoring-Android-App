package marchandivan.RoomMonitoring.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;

import marchandivan.RoomMonitoring.MainActivity;
import marchandivan.RoomMonitoring.R;
import marchandivan.RoomMonitoring.SensorConnectionActivity;
import marchandivan.RoomMonitoring.adapter.SensorListAdapter;
import marchandivan.RoomMonitoring.db.DeviceConfig;
import marchandivan.RoomMonitoring.db.SensorConfig;
import marchandivan.RoomMonitoring.receiver.MonitorRoomReceiver;

/**
 * Created by ivan on 9/8/16.
 */
public class SensorDialogBuilder {
    private Context mContext;
    private AlertDialog.Builder mBuilder;
    private Runnable mAddSensorCallback;

    public SensorDialogBuilder(Context context, Runnable addSensorCallback) {
        mContext = context;
        mBuilder = new AlertDialog.Builder(context);
        mAddSensorCallback = addSensorCallback;
    }

    public AlertDialog create() {

        // Get list of sensor
        SensorListAdapter sensorListAdapter = new SensorListAdapter(mContext);

        if (sensorListAdapter.isEmpty()) {
            mBuilder.setTitle(R.string.no_sensor_to_add);
            mBuilder.setPositiveButton(R.string.ok, null);
        } else {
            // Inflate layout
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View dialogView = inflater.inflate(R.layout.add_sensor_dialog, null);

            // Device selector
            final Spinner deviceSelector = (Spinner) dialogView.findViewById(R.id.sensor_selector);
            deviceSelector.setAdapter(sensorListAdapter);

            // Build the dialog
            mBuilder.setView(dialogView);
            mBuilder.setTitle(R.string.add_sensor);

            // Add cancel button
            mBuilder.setNegativeButton(R.string.cancel, null);
            mBuilder.setPositiveButton(R.string.add_sensor, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Spinner deviceSelector = (Spinner) dialogView.findViewById(R.id.sensor_selector);
                    SensorConfig sensorConfig = (SensorConfig) deviceSelector.getSelectedItem();
                    sensorConfig.updateVisibility(true);

                    // Call callback
                    mAddSensorCallback.run();
                }
            });
        }

        return mBuilder.create();

    }

}