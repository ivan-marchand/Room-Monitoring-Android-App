package marchandivan.RoomMonitoring;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A placeholder fragment containing a simple view.
 */
public class RoomListFragment extends Fragment {
    private String mRoom;
    private View mView;

    public RoomListFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle args = getArguments();
        mRoom = args.getString("room");
        mView = inflater.inflate(R.layout.fragment_room_list, container, false);
        // Set room name
        TextView roomName = (TextView)mView.findViewById(R.id.room_name);
        roomName.setText(mRoom);

        return mView;
    }

    public void updateView(Float temperature, Float humidity) {
        if (mView != null) {
            // Temperature
            TextView temperatureView = (TextView)mView.findViewById(R.id.temperature);
            temperatureView.setText(String.format("%.1f F", temperature));
            // Humidity
            TextView humidityView = (TextView)mView.findViewById(R.id.humidity);
            humidityView.setText(String.format("%.1f F", humidity));
        }
    }
}
