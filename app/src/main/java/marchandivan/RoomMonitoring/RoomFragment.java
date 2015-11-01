package marchandivan.RoomMonitoring;

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

import marchandivan.RoomMonitoring.db.AlarmConfig;
import marchandivan.RoomMonitoring.db.RoomConfig;

/**
 * A placeholder fragment containing a simple view.
 */
public class RoomFragment extends Fragment implements View.OnClickListener {
    private String mRoom;
    private View mView;
    private Fragment mFragment;

    public RoomFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mFragment = this;
        Bundle args = getArguments();
        mRoom = args.getString("room");
        mView = inflater.inflate(R.layout.fragment_room, container, false);

        // Set room name, capitalized (First letter upper case)
        TextView roomName = (TextView)mView.findViewById(R.id.room_name);
        char[] roomCapitalized = mRoom.toCharArray();
        roomCapitalized[0] = Character.toUpperCase(roomCapitalized[0]);
        roomName.setText(new String(roomCapitalized));

        // Update temperature and humidity
        updateView(args.containsKey("temperature") ? args.getFloat("temperature") : null,
                   args.containsKey("humidity") ? args.getFloat("humidity") : null);

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
                args.putString("room", mRoom);
                roomDetailsIntent.putExtras(args);
                v.getContext().startActivity(roomDetailsIntent);
            }
        });

        return mView;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.remove_room:
                // Build the dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setTitle("Are you sure you want to remove room " + mRoom + "?");
                builder.setNegativeButton("Cancel", null);
                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Remove room & alarm config
                        RoomConfig roomConfig = new RoomConfig(mFragment.getActivity(), mRoom);
                        roomConfig.delete();
                        AlarmConfig alarmConfig = new AlarmConfig(mFragment.getActivity(), mRoom);
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

    public void updateView(Float temperature, Float humidity) {
        if (mView != null) {
            if (temperature != null) {
                // Temperature
                TextView temperatureView = (TextView)mView.findViewById(R.id.temperature);
                temperatureView.setText(String.format("%.1f F", temperature));
            }
            if (humidity != null) {
                // Humidity
                TextView humidityView = (TextView)mView.findViewById(R.id.humidity);
                humidityView.setText(String.format("%.1f %%", humidity));
            }
        }
    }
}
