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

import marchandivan.RoomMonitoring.R;
import marchandivan.RoomMonitoring.db.DeviceConfig;
import marchandivan.RoomMonitoring.db.SensorConfig;
import marchandivan.RoomMonitoring.sensor.Sensor;
import marchandivan.RoomMonitoring.sensor.SensorFactory;

/**
 * Created by ivan on 9/9/16.
 */
public class SensorListAdapter extends BaseAdapter implements SpinnerAdapter {
    ArrayList<SensorConfig> mSensorList = new ArrayList<>();
    Context mContext;

    public SensorListAdapter(Context context) {
        mContext = context;
        for (SensorConfig sensorConfig: SensorConfig.GetMap(context).values()) {
            if (!sensorConfig.isVisible()) {
                mSensorList.add(sensorConfig);
            }
        }
    }

    @Override
    public int getCount() {
        return mSensorList.size();
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public Object getItem(int position) {
        return mSensorList.get(position);
    }

    @Override
    public View getView(int position, View recycle, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View sensorItemView = inflater.inflate(R.layout.device_spinner_item, parent, false);

        // Get corresponding sensor / device
        SensorConfig sensorConfig = (SensorConfig) getItem(position);
        DeviceConfig deviceConfig = new DeviceConfig(mContext, sensorConfig.getDeviceId());
        deviceConfig.read();
        Sensor sensor = SensorFactory.Get(deviceConfig.getType());

        // Icon
        ImageView icon = (ImageView) sensorItemView.findViewById(R.id.device_icon);
        icon.setImageResource(sensor.getIcon());

        // Name
        TextView name = (TextView) sensorItemView.findViewById(R.id.device_name);
        name.setText(sensorConfig.getName());
        return sensorItemView;
    }
}
