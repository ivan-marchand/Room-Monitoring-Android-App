package marchandivan.RoomMonitoring;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import org.json.JSONException;
import org.json.JSONObject;

import marchandivan.RoomMonitoring.db.RoomConfig;
import marchandivan.RoomMonitoring.receiver.MonitorRoomReceiver;

public class MainActivity extends AppCompatActivity {
    private Handler mHandler;

    // Display refresh rate (in ms)
    private int mDisplayRefreshInterval = 30 * 1000; // Every 30s

    private void updateDisplay() throws JSONException {
        // Add new room fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        for (RoomConfig roomConfig : MonitorRoomReceiver.GetRoomConfigs().values()) {
            RoomFragment fragment = new RoomFragment();
            // Pass the name as parameter
            Bundle args = new Bundle();
            args.putString("room", roomConfig.mRoomName);
            if (MonitorRoomReceiver.GetRooms().containsKey(roomConfig.mRoomName)) {
                JSONObject room = MonitorRoomReceiver.GetRooms().get(roomConfig.mRoomName);
                args.putFloat("temperature", Float.parseFloat(room.getString("temperature")));
                args.putFloat("humidity", Float.parseFloat(room.getString("humidity")));
            }
            fragment.setArguments(args);
            // Add fragment to main activity
            String aTag = "room_list_fragment_" + roomConfig.mRoomName;
            if (fragmentManager.findFragmentByTag(aTag) != null) {
                Log.d("updateDisplay", "Replace fragment for " + roomConfig.mRoomName);
                fragmentTransaction.replace(R.id.room_list_table, fragment, aTag);
            }
            else {
                Log.d("updateDisplay", "Add fragment for " + roomConfig.mRoomName);
                fragmentTransaction.add(R.id.room_list_table, fragment, aTag);
            }
        }
        // Something to commit?
        if (!fragmentTransaction.isEmpty()) {
            fragmentTransaction.commit();
        }
    }

    private Runnable mDisplayRefresher = new Runnable() {
        @Override
        public void run() {
            try {
                if (hasWindowFocus()) {
                    updateDisplay();
                }
            } catch (Exception e) {

            } finally {
                mHandler.removeCallbacks(this);
                mHandler.postDelayed(this, MonitorRoomReceiver.GetRooms().isEmpty() ? 1000 : mDisplayRefreshInterval);
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

        // Get current room temp
        MonitorRoomReceiver.Update(this);

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
        if (id == R.id.action_settings) {
            openSettings();
            return true;
        } else if (id == R.id.action_add_room) {
            manageRooms();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void manageRooms() {

    }
}
