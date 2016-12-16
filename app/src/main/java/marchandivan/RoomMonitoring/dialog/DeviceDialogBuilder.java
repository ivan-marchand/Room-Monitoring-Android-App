package marchandivan.RoomMonitoring.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.ArrayList;

import marchandivan.RoomMonitoring.DeviceConnectionActivity;
import marchandivan.RoomMonitoring.R;
import marchandivan.RoomMonitoring.adapter.DeviceListAdapter;
import marchandivan.RoomMonitoring.sensor.Sensor;
import marchandivan.RoomMonitoring.sensor.SensorFactory;

/**
 * Created by ivan on 9/8/16.
 */
public class DeviceDialogBuilder {
    private Context mContext;
    private AlertDialog.Builder mBuilder;

    public DeviceDialogBuilder(Context context) {
        mContext = context;
        mBuilder = new AlertDialog.Builder(context);
    }

    public AlertDialog create() {
        // Inflate layout
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View dialogView = inflater.inflate(R.layout.add_device_dialog, null);

        // Device selector
        final Spinner spinner = (Spinner)dialogView.findViewById(R.id.device_type_selector);
        DeviceListAdapter adapter = new DeviceListAdapter(mContext);
        spinner.setAdapter(adapter);

        // Build the dialog
        mBuilder.setTitle(R.string.add_sensor);
        mBuilder.setView(dialogView);

        // Add confirm/cancel buttons
        mBuilder.setPositiveButton(R.string.add_device, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(mContext, DeviceConnectionActivity.class);
                Sensor sensor = (Sensor) spinner.getSelectedItem();
                intent.putExtra("sensorType", sensor.getDeviceType());
                mContext.startActivity(intent);
            }
        });
        // Add confirm/cancel buttons
        mBuilder.setNegativeButton(R.string.cancel, null);

        return mBuilder.create();
    }}
