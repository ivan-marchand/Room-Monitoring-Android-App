package marchandivan.RoomMonitoring.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import marchandivan.RoomMonitoring.R;
import marchandivan.RoomMonitoring.SensorDetailsActivity;
import marchandivan.RoomMonitoring.db.AlarmConfig;
import marchandivan.RoomMonitoring.db.SensorConfig;
import marchandivan.RoomMonitoring.receiver.MonitorRoomReceiver;

/**
 * A placeholder fragment containing a simple view.
 */
public class SensorFragment extends Fragment implements View.OnClickListener {
    private SensorConfig mSensorConfig;
    private View mView;
    private Fragment mFragment;
    private Boolean mAlarmActivated = false;

    public SensorFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mFragment = this;
        Bundle args = getArguments();
        mSensorConfig = new SensorConfig(getContext(), args.getLong("sensor_id"));
        mSensorConfig.read();
        mView = inflater.inflate(R.layout.fragment_sensor, container, false);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        mAlarmActivated = sharedPreferences.getBoolean("temperature_alarm", false);

        // Set room name, capitalized (First letter upper case)
        TextView roomName = (TextView)mView.findViewById(R.id.sensor_name);
        roomName.setText(mSensorConfig.getName());

        // Set icon
        ImageView icon = (ImageView)mView.findViewById(R.id.sensor_header_icon);
        switch (mSensorConfig.getType()) {
            case THERMOSTAT:
                icon.setImageResource(R.drawable.thermostat);
                break;
            case THERMOMETER:
            default:
                icon.setImageResource(R.drawable.thermometer);
                break;
        }

        // Alarm?
        ImageView alarmIcon = (ImageView) mView.findViewById(R.id.alarm_icon);
        if (mAlarmActivated) {
            AlarmConfig alarmConfig = new AlarmConfig(getContext(), mSensorConfig.getId());
            ArrayList<AlarmConfig.Alarm> alarms = alarmConfig.read();
            alarmIcon.setVisibility(alarms.isEmpty() ? View.GONE : View.VISIBLE);
        } else {
            alarmIcon.setVisibility(View.GONE);
        }

        // Update temperature and humidity
        registerViews();


        // Show room details button
        LinearLayout sensorFragment = (LinearLayout) mView.findViewById(R.id.sensor_fragment);
        sensorFragment.setOnClickListener(this);

        return mView;
    }

    @Override
    public void onDestroyView() {
        unRegisterViews();
        super.onDestroyView();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sensor_fragment:
                // Show room details
                Intent roomDetailsIntent = new Intent(v.getContext(), SensorDetailsActivity.class);
                Bundle args = new Bundle();
                args.putLong("sensor_id", mSensorConfig.getId());
                roomDetailsIntent.putExtras(args);
                v.getContext().startActivity(roomDetailsIntent);
                break;
            default:
                break;
        }
    }

    private void removeSensor(View v) {
        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
        builder.setTitle("Are you sure you want to remove sensor " + mSensorConfig.getName() + "?");
        builder.setNegativeButton("Cancel", null);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Remove room & alarm config
                mSensorConfig.updateVisibility(false);
                AlarmConfig alarmConfig = new AlarmConfig(mFragment.getActivity(), mSensorConfig.getId());
                alarmConfig.delete();

                // Remove fragment
                FragmentManager fragmentManager = mFragment.getActivity().getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.remove(mFragment);
                fragmentTransaction.commit();
            }
        });

        // Show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void registerViews() {
        if (mView != null) {
            // Temperature
            TextView temperatureView = (TextView)mView.findViewById(R.id.temperature);
            // Humidity
            TextView humidityView = (TextView)mView.findViewById(R.id.humidity);
            MonitorRoomReceiver.Register(mSensorConfig.getId(), "main_view", temperatureView, humidityView);

        }
    }

    private void unRegisterViews() {
        MonitorRoomReceiver.Unregister(mSensorConfig.getId(), "main_view");
    }
}
