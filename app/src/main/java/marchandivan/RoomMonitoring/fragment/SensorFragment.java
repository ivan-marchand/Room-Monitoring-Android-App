package marchandivan.RoomMonitoring.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import marchandivan.RoomMonitoring.R;
import marchandivan.RoomMonitoring.RoomDetailsActivity;
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

        // Set room name, capitalized (First letter upper case)
        TextView roomName = (TextView)mView.findViewById(R.id.room_name);
        roomName.setText(mSensorConfig.getName());

        // Update temperature and humidity
        registerViews();

        // Remove room button
        ImageButton removeRoomButton = (ImageButton) mView.findViewById(R.id.remove_room);
        removeRoomButton.setOnClickListener(this);

        // Show room details button
        ImageButton roomDetailsButton = (ImageButton) mView.findViewById(R.id.room_details_button);
        roomDetailsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show room details
                Intent roomDetailsIntent = new Intent(v.getContext(), RoomDetailsActivity.class);
                Bundle args = new Bundle();
                args.putLong("sensor_id", mSensorConfig.getId());
                roomDetailsIntent.putExtras(args);
                v.getContext().startActivity(roomDetailsIntent);
            }
        });

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
            case R.id.remove_room:
                // Build the dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setTitle("Are you sure you want to remove sensor " + mSensorConfig.getName() + "?");
                builder.setNegativeButton("Cancel", null);
                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Remove room & alarm config
                        mSensorConfig.delete();
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
                break;
            default:
                break;
        }
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
        MonitorRoomReceiver.RemovePreUpdateCallback(mSensorConfig.getId(), "main_view");
        MonitorRoomReceiver.RemovePostUpdateCallback(mSensorConfig.getId(), "main_view");
    }
}
