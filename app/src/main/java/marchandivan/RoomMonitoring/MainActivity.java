package marchandivan.RoomMonitoring;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private Handler mHandler;
    private HashMap<String, RoomListFragment> mFragmentMap = new HashMap<String, RoomListFragment>();

    // Display refresh rate (in ms)
    private int mDisplayRefreshInterval = 30 * 1000; // Every 30s

    private void updateDisplay(HashMap<String, Float> temperatureMap, HashMap<String, Float> humidityMap) {
        // Add new room fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        for (String room : temperatureMap.keySet()) {
            // New fragment ?
            if (!mFragmentMap.containsKey(room)) {
                RoomListFragment fragment = new RoomListFragment();
                // Pass the name as parameter
                Bundle args = new Bundle();
                args.putString("room", room);
                args.putFloat("temperature", temperatureMap.get(room));
                args.putFloat("humidity", humidityMap.get(room));
                fragment.setArguments(args);
                // Add fragment to main activity
                fragmentTransaction.add(R.id.room_list_table, fragment);
                // Add fragment to the map of existing fragment
                mFragmentMap.put(room, fragment);
            }
            else {
                // Update the view
                mFragmentMap.get(room).updateView(temperatureMap.get(room), humidityMap.get(room));
            }
        }
        fragmentTransaction.commit();
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

}
