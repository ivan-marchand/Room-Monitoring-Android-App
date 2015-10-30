package marchandivan.RoomMonitoring;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import marchandivan.RoomMonitoring.db.RoomConfig;

/**
 * A placeholder fragment containing a simple view.
 */
public class RoomFragment extends Fragment implements View.OnClickListener {
    private String mRoom;
    private View mView;

    public RoomFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle args = getArguments();
        mRoom = args.getString("room");
        mView = inflater.inflate(R.layout.fragment_room_list, container, false);

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
        Button roomDetailsButton = (Button) mView.findViewById(R.id.room_details_button);
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
                RoomConfig roomConfig = new RoomConfig(v.getContext(), mRoom);
                roomConfig.delete();
                // Remove fragment
                FragmentManager fragmentManager = this.getActivity().getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.remove(this);
                fragmentTransaction.commit();
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
