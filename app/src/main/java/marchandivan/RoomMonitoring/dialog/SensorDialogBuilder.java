package marchandivan.RoomMonitoring.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;

import marchandivan.RoomMonitoring.R;
import marchandivan.RoomMonitoring.SensorConnectionActivity;
import marchandivan.RoomMonitoring.adapter.DeviceConfigListAdapter;
import marchandivan.RoomMonitoring.db.DeviceConfig;

/**
 * Created by ivan on 9/8/16.
 */
public class SensorDialogBuilder {
    private Context mContext;
    private AlertDialog.Builder mBuilder;

    public SensorDialogBuilder(Context context) {
        mContext = context;
        mBuilder = new AlertDialog.Builder(context);
    }

    public void show() {

        // Inflate layout
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View dialogView = inflater.inflate(R.layout.add_sensor_dialog, null);

        // Device selector
        final Spinner deviceSelector = (Spinner) dialogView.findViewById(R.id.device_selector);
        DeviceConfigListAdapter deviceConfigListAdapter = new DeviceConfigListAdapter(mContext);
        deviceSelector.setAdapter(deviceConfigListAdapter);


        // Build the dialog
        mBuilder.setView(dialogView);
        mBuilder.setTitle(R.string.add_sensor);

        // Add confirm/cancel buttons
        mBuilder.setPositiveButton(R.string.add_sensor, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Spinner deviceSelector = (Spinner) dialogView.findViewById(R.id.device_selector);
                DeviceConfig deviceConfig = (DeviceConfig) deviceSelector.getSelectedItem();
                Intent intent = new Intent(mContext, SensorConnectionActivity.class);
                intent.putExtra("sensorId", deviceConfig.getId());
                mContext.startActivity(intent);
            }
        });
        // Add confirm/cancel buttons
        mBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        final AlertDialog alertDialog = mBuilder.create();
        // Add new device
        Button addNewButton = (Button) dialogView.findViewById(R.id.add_device);
        addNewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Add Device dialog
                DeviceDialogBuilder deviceDialogBuilder = new DeviceDialogBuilder(mContext);
                AlertDialog deviceDialog = deviceDialogBuilder.create();
                deviceDialog.show();
                alertDialog.dismiss();
            }
        });

        alertDialog.show();
    }

}