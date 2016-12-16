package marchandivan.RoomMonitoring.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedList;

import marchandivan.RoomMonitoring.R;
import marchandivan.RoomMonitoring.db.DeviceConfig;
import marchandivan.RoomMonitoring.sensor.Sensor;
import marchandivan.RoomMonitoring.sensor.SensorFactory;

/**
 * Created by ivan on 9/9/16.
 */
public class DeviceListAdapter extends BaseAdapter implements SpinnerAdapter {
    ArrayList<Sensor> mDeviceList;
    Context mContext;

    public DeviceListAdapter(Context context) {
        mContext = context;
        mDeviceList = new ArrayList<Sensor>(SensorFactory.GetSensorMap().values());
    }

    @Override
    public int getCount() {
        return mDeviceList.size();
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public Object getItem(int position) {
        return mDeviceList.get(position);
    }

    @Override
    public View getView(int position, View recycle, ViewGroup parent) {
        // No recycled view, inflate the "original" from the platform:
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View deviceItemView = inflater.inflate(R.layout.device_spinner_item, parent, false);
        // Get corresponding sensor
        Sensor sensor = (Sensor) getItem(position);
        // Icon
        ImageView icon = (ImageView) deviceItemView.findViewById(R.id.device_icon);
        icon.setImageResource(sensor.getIcon());
        // Name
        TextView name = (TextView) deviceItemView.findViewById(R.id.device_name);
        name.setText(sensor.getDisplayName());

        return deviceItemView;
    }
}
