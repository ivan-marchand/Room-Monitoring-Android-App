package marchandivan.RoomMonitoring;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

/**
 * A placeholder fragment containing a simple view.
 */
public class RoomListFragment extends Fragment implements View.OnClickListener {
    private String mRoom;
    private View mView;

    // REST client, used for IR commands
    private RestClient mRestClient;

    public RoomListFragment() {
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
        updateView(args.getFloat("temperature"), args.getFloat("humidity"));

        // Setup click listener
        Button tempPlus = (Button) mView.findViewById(R.id.IRButton_TEMPPLUS);
        tempPlus.setOnClickListener(this);
        Button tempMinus = (Button) mView.findViewById(R.id.IRButton_TEMPMINUS);
        tempMinus.setOnClickListener(this);
        Button onOff = (Button) mView.findViewById(R.id.IRButton_ONOFF);
        onOff.setOnClickListener(this);

        // Configure the REST client
        mRestClient = new RestClient(getActivity().getAssets());
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mRestClient.configure(sharedPref);


        return mView;
    }

    protected class SendIRCommand extends AsyncTask<String, Void, JSONObject> {

        protected JSONObject doInBackground(String... strings) {
            return mRestClient.get("/api/v1/sendIRCommand/"+ mRoom + "/" + strings[0]);
        }

        protected void onPostExecute(JSONObject result) {
            try {
                Log.d("RestClient", result.toString());
                if (result.has("result") && result.getString("result").equals("Success")) {
                    Toast.makeText(getActivity(), "IR Command successfully sent!", Toast.LENGTH_SHORT).show();
                } else {
                    String errorMessage = "Failed to send IR Command";
                    if (result.has("error")) {
                        errorMessage += ", " + result.getString("error");
                    }
                    Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
            catch (Exception e) {
                Log.d("SendIRCommand", "Error " + e.toString());
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.IRButton_ONOFF:
                new SendIRCommand().execute("ONOFF");
                break;
            case R.id.IRButton_TEMPPLUS:
                new SendIRCommand().execute("TEMPPLUS");
                break;
            case R.id.IRButton_TEMPMINUS:
                new SendIRCommand().execute("TEMPMINUS");
                break;
            default:
                break;
        }
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
