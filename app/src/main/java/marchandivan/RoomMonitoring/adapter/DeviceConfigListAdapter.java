package marchandivan.RoomMonitoring.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import marchandivan.RoomMonitoring.db.DeviceConfig;

/**
 * Created by ivan on 9/9/16.
 */
public class DeviceConfigListAdapter extends BaseAdapter implements SpinnerAdapter {
    ArrayList<DeviceConfig> mDeviceConfigList;
    Context mContext;

    public DeviceConfigListAdapter(Context context) {
        mContext = context;
        mDeviceConfigList = new ArrayList<DeviceConfig>(DeviceConfig.GetMap(mContext).values());
    }

    @Override
    public int getCount() {
        return mDeviceConfigList.size();
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public Object getItem(int position) {
        return mDeviceConfigList.get(position);
    }

    @Override
    public View getView(int position, View recycle, ViewGroup parent) {
        TextView text;
        if (recycle != null) {
            // Re-use the recycled view here!
            text = (TextView) recycle;
        } else {
            // No recycled view, inflate the "original" from the platform:
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            text = (TextView) inflater.inflate(
                    android.R.layout.simple_dropdown_item_1line, parent, false
            );
        }
        text.setText(mDeviceConfigList.get(position).getName());
        return text;
    }
}
