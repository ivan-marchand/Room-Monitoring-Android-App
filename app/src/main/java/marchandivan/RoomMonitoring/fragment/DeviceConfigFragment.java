package marchandivan.RoomMonitoring.fragment;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import marchandivan.RoomMonitoring.R;
import marchandivan.RoomMonitoring.db.DeviceConfig;
import marchandivan.RoomMonitoring.db.SensorConfig;
import marchandivan.RoomMonitoring.sensor.Sensor;
import marchandivan.RoomMonitoring.sensor.SensorFactory;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link DeviceConfigFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link DeviceConfigFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DeviceConfigFragment extends Fragment {
    private static final String ARG_DEVICE_ID = "device_id";

    private DeviceConfig mDeviceConfig;
    private Sensor mSensor;

    public DeviceConfigFragment() {
        // Required empty public constructor
    }

    private void displaySensors() {
        try {
            Log.d("DeviceConfigFragment", "updateDisplay");
            // Add new room fragment
            FragmentManager fragmentManager = getChildFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            for (SensorConfig sensorConfig: SensorConfig.GetList(getContext(), mDeviceConfig.getId())) {
                // Should be visible?
                SensorConfigFragment fragment = SensorConfigFragment.newInstance(sensorConfig.getId());
                // Add fragment to main activity
                String aTag = "sensor_list_fragment_" + sensorConfig.getId() + "_" + sensorConfig.getName();
                if (fragmentManager.findFragmentByTag(aTag) == null) {
                    Log.d("updateDisplay", "Add fragment for " + aTag);
                    fragmentTransaction.add(R.id.sensor_list, fragment, aTag);
                }
            }

            // Something to commit?
            if (!fragmentTransaction.isEmpty()) {
                fragmentTransaction.commit();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param deviceId Device Id.
     * @return A new instance of fragment DeviceConfigFragment.
     */
    public static DeviceConfigFragment newInstance(long deviceId) {
        DeviceConfigFragment fragment = new DeviceConfigFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_DEVICE_ID, deviceId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mDeviceConfig = new DeviceConfig(getContext(), getArguments().getLong(ARG_DEVICE_ID));
            mDeviceConfig.read();
            mSensor = SensorFactory.Get(mDeviceConfig.getType());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_device_config, container, false);

        // Icon
        ImageView icon = (ImageView)view.findViewById(R.id.device_icon);
        icon.setImageResource(mSensor.getIcon());

        // Name
        TextView deviceName = (TextView)view.findViewById(R.id.device_name);
        deviceName.setText(mDeviceConfig.getName());

        // Sensors
        displaySensors();
        return view;
    }

}
