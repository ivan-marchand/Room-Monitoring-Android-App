package marchandivan.RoomMonitoring.fragment;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import marchandivan.RoomMonitoring.R;
import marchandivan.RoomMonitoring.http.RestClient;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link DeviceFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link DeviceFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DeviceFragment extends Fragment implements View.OnClickListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_TYPE = "type";
    private static final String ARG_NAME = "name";
    private static final String ARG_COMMANDS = "commands";

    // REST client, used for IR commands
    private RestClient mRestClient;

    // Device details
    private String mType;
    private String mName;
    private JSONArray mCommands;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment DeviceFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static DeviceFragment newInstance(String type, String name, JSONArray commands) {
        DeviceFragment fragment = new DeviceFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TYPE, type);
        args.putString(ARG_NAME, name);
        args.putString(ARG_COMMANDS, commands.toString());
        fragment.setArguments(args);
        return fragment;
    }

    public DeviceFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            if (getArguments() != null) {
                mType = getArguments().getString(ARG_TYPE);
                mName = getArguments().getString(ARG_NAME);
                mCommands = new JSONArray(getArguments().getString(ARG_COMMANDS));
            }
        }
        catch (Exception e) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = null;
        try {
            // Inflate the layout for this fragment
            view = inflater.inflate(R.layout.fragment_device, container, false);

            // Update Device icon/name
            ImageView deviceIcon = (ImageView)view.findViewById(R.id.device_icon);
            if (mType.equals("H")) {
                deviceIcon.setImageResource(R.drawable.heater);
            } else if (mType.equals("A")) {
                deviceIcon.setImageResource(R.drawable.ac);
            }
            TextView deviceName = (TextView)view.findViewById(R.id.device_name);
            char[] deviceCapitalized = mName.toCharArray();
            deviceCapitalized[0] = Character.toUpperCase(deviceCapitalized[0]);
            deviceName.setText(new String(deviceCapitalized));

            // Add commands
            LinearLayout buttonContainer = (LinearLayout)view.findViewById(R.id.commands);
            for (int i = 0 ; i < mCommands.length() ; i++) {
                JSONObject command = mCommands.getJSONObject(i);
                Button button = new Button(view.getContext());
                button.setText(command.getString("text"));
                button.setId(i);
                button.setOnClickListener(this);
                buttonContainer.addView(button);
            }

            // Configure the REST client
            //mRestClient = new RestClient(this.getActivity());
            //SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        } catch (Exception e) {

        }
        return view;
    }

    protected class SendIRCommand extends AsyncTask<String, Void, JSONObject> {

        protected JSONObject doInBackground(String... strings) {
            return mRestClient.getJson("/api/v1/sendIRCommand/"+ mName + "/" + strings[0]);
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
        try {
            String commandName = mCommands.getJSONObject(v.getId()).getString("name");
            new SendIRCommand().execute(commandName);
        } catch (Exception e) {
            Log.d("DeviceDetails", "Error " + e.getMessage());
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

}
