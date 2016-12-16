package marchandivan.RoomMonitoring.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import marchandivan.RoomMonitoring.R;
import marchandivan.RoomMonitoring.db.SensorConfig;

/**
 * Created by ivan on 11/2/16.
 */

public class ThermostatModeAdapter extends BaseAdapter implements SpinnerAdapter {
    Context mContext;

    public ThermostatModeAdapter(Context context) {
        mContext = context;
    }

    private int getIcon(int position) {
        switch (position) {
            case 0:
                return R.drawable.ic_power;
            case 1:
                return R.drawable.heater;
            case 2:
                return R.drawable.ac;
        }
        return 0;
    }

    public int getPosition(SensorConfig.ThermostatMode mode) {
        if (mode != null) {
            switch (mode) {
                case OFF:
                    return 0;
                case HEAT:
                    return 1;
                case COOL:
                    return 2;
            }
        }
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View thermostatModeView = inflater.inflate(R.layout.thermostat_mode_spinner, parent, false);
        // Set icon
        ImageView icon = (ImageView) thermostatModeView.findViewById(R.id.icon);
        icon.setImageResource(getIcon(position));
        // Set Name
        TextView mode = (TextView) thermostatModeView.findViewById(R.id.thermostat_mode_name);
        switch (position) {
            case 0:
                mode.setText(mContext.getString(R.string.off));
                break;
            case 1:
                mode.setText(mContext.getString(R.string.heater));
                break;
            case 2:
                mode.setText(mContext.getString(R.string.ac));
                break;
        }
        return thermostatModeView;
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public Object getItem(int position) {
        switch (position) {
            case 0:
                return SensorConfig.ThermostatMode.OFF;
            case 1:
                return SensorConfig.ThermostatMode.HEAT;
            case 2:
                return SensorConfig.ThermostatMode.COOL;
        }
        return SensorConfig.ThermostatMode.OFF;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


}
