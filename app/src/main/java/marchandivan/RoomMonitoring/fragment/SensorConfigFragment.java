package marchandivan.RoomMonitoring.fragment;

import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import marchandivan.RoomMonitoring.db.SensorConfig;
import marchandivan.RoomMonitoring.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SensorConfigFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link SensorConfigFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SensorConfigFragment extends Fragment {
    private static final String ARG_SENSOR_ID = "sensord_id";

    private SensorConfig mSensorConfig;

    public SensorConfigFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param sensorId Sensor Id.
     * @return A new instance of fragment SensorConfigFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SensorConfigFragment newInstance(long sensorId) {
        SensorConfigFragment fragment = new SensorConfigFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_SENSOR_ID, sensorId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mSensorConfig = new SensorConfig(getContext(), getArguments().getLong(ARG_SENSOR_ID));
            mSensorConfig.read();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_sensor_config, container, false);

        // Icon
        ImageView icon = (ImageView)view.findViewById(R.id.sensor_icon);
        icon.setImageResource(mSensorConfig.getType() == SensorConfig.Type.THERMOMETER ? R.drawable.thermometer : R.drawable.thermostat);

        // Name
        TextView sensorName = (TextView)view.findViewById(R.id.sensor_name);
        sensorName.setText(mSensorConfig.getName());

        // Visibility
        final ImageButton sensorVisibility = (ImageButton)view.findViewById(R.id.sensor_visibility);
        sensorVisibility.setImageResource(mSensorConfig.isVisible() ? R.drawable.ic_visible : R.drawable.ic_visibility_off);
        sensorVisibility.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSensorConfig.isVisible()) {
                    mSensorConfig.updateVisibility(false);
                    sensorVisibility.setImageResource(R.drawable.ic_visibility_off);
                } else {
                    mSensorConfig.updateVisibility(true);
                    sensorVisibility.setImageResource(R.drawable.ic_visible);
                }
            }
        });
        return view;
    }

    public void updateVisibility(ImageButton imageButton) {

    }
}
