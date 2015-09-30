package marchandivan.RoomMonitoring;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private Handler mHandler;

    // Display refresh rate (in ms)
    private int mDisplayRefreshInterval = 30 * 1000; // Every 30s

    // REST client, used for IR commands
    private RestClient mRestClient;

    private void updateDisplay(HashMap<String, Float> temperatureMap, HashMap<String, Float> humidityMap) {
        // Salon
        if (temperatureMap.containsKey("salon")) {
            TextView salonTemperatureView = (TextView) findViewById(R.id.salon_temperature);
            salonTemperatureView.setText(String.format("%.1f F", temperatureMap.get("salon")));
        }
        if (humidityMap.containsKey("salon")) {
            TextView salonHumidityView = (TextView) findViewById(R.id.salon_humidity);
            salonHumidityView.setText(String.format("%.1f %%", humidityMap.get("salon")));
        }

        // Noah
        if (temperatureMap.containsKey("noah")) {
            TextView noahTemperatureView = (TextView) findViewById(R.id.noah_temperature);
            noahTemperatureView.setText(String.format("%.1f F", temperatureMap.get("noah")));
        }
        if (humidityMap.containsKey("noah")) {
            TextView noahHumidityView  = (TextView) findViewById(R.id.noah_humidity);
            noahHumidityView.setText(String.format("%.1f %%", humidityMap.get("noah")));
        }
    }

    private Runnable mDisplayRefresher = new Runnable() {
        @Override
        public void run() {
            try {
                updateDisplay(MonitorRoomReceiver.GetTemperatures(), MonitorRoomReceiver.GetHumidities());
            } finally {
                mHandler.removeCallbacks(this);
                mHandler.postDelayed(this, mDisplayRefreshInterval);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the layout
        setContentView(marchandivan.RoomMonitoring.R.layout.activity_main);

        // Init the callback handler
        mHandler = new Handler();

        // Configure the REST client
        mRestClient = new RestClient(getAssets());
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mRestClient.configure(sharedPref);

        // Activate the room monitoring
        MonitorRoomReceiver.Activate(this);

        // Run the display refresher
        mDisplayRefresher.run();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // On destroy, if alarm is not activated... no need to keep the monitoring running
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!sharedPreferences.getBoolean("temperature_alarm", false)) {
            MonitorRoomReceiver.Deactivate(this);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(marchandivan.RoomMonitoring.R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == marchandivan.RoomMonitoring.R.id.action_settings) {
            openSettings();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    protected class SendIRCommand extends AsyncTask<String, Void, JSONObject> {

        protected JSONObject doInBackground(String... strings) {
            return mRestClient.get("/api/v1/sendIRCommand/salon/" + strings[0]);
        }

        protected void onPostExecute(JSONObject result) {
            try {
                Log.d("RestClient", result.toString());
                if (result.has("result") && result.getString("result").equals("Success")) {
                    Toast.makeText(MainActivity.this, "IR Command successfully sent!", Toast.LENGTH_SHORT).show();
                } else {
                    String errorMessage = "Failed to send IR Command";
                    if (result.has("error")) {
                        errorMessage += ", " + result.getString("error");
                    }
                    Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
            catch (Exception e) {
                Log.d("SendIRCommand", "Error " + e.toString());
            }
        }
    }

    public void sendIRCommand(View view) {
        switch (view.getId()) {
            case marchandivan.RoomMonitoring.R.id.salon_IRButton_ONOFF:
                new SendIRCommand().execute("ONOFF");
                break;
            case marchandivan.RoomMonitoring.R.id.salon_IRButton_TEMPPLUS:
                new SendIRCommand().execute("TEMPPLUS");
                break;
            case marchandivan.RoomMonitoring.R.id.salon_IRButton_TEMPMINUS:
                new SendIRCommand().execute("TEMPMINUS");
                break;
            default:
                break;
        }
    }
}
