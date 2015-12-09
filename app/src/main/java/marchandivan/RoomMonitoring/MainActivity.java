package marchandivan.RoomMonitoring;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import marchandivan.RoomMonitoring.db.RoomConfig;
import marchandivan.RoomMonitoring.fragment.RoomFragment;
import marchandivan.RoomMonitoring.receiver.MonitorRoomReceiver;

public class MainActivity extends AppCompatActivity {
    private Handler mHandler;

    // Display refresh rate (in ms)
    private int mDisplayRefreshInterval = 30 * 1000; // Every 30s

    private void updateDisplay() {
        try {
            // Add new room fragment
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            for (RoomConfig roomConfig : RoomConfig.GetMap(this).values()) {
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
        catch (Exception e) {

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
                e.printStackTrace();
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

        // Activate the room monitoring
        MonitorRoomReceiver.Activate(this);

        // Run the display refresher
        updateDisplay();
        mDisplayRefresher.run();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Get current room temp
        MonitorRoomReceiver.Update(this);

        // Update the display
        updateDisplay();
    }

    @Override
    protected void onDestroy() {

        // On destroy, if alarm is not activated... no need to keep the monitoring running
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!sharedPreferences.getBoolean("temperature_alarm", false)) {
            MonitorRoomReceiver.Deactivate(this);
        }

        super.onDestroy();
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
        }

        return super.onOptionsItemSelected(item);
    }

    public void addRoom(View view) {
        // Get the list of room that can be added
        List<String> rooms = new ArrayList<String>();
        for(String room: MonitorRoomReceiver.GetRooms().keySet()) {
            Log.d("addRoom", room);
            if(!RoomConfig.GetMap(view.getContext()).containsKey(room)) {
                rooms.add(room);
            }
        }
        Log.d("addRoom", rooms.toString());

        if (!rooms.isEmpty()) {
            // Build the dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select a room");
            // Inflate layout
            LayoutInflater inflater = this.getLayoutInflater();
            final View dialogView = inflater.inflate(R.layout.add_room_dialog, null);
            final Spinner spinner = (Spinner)dialogView.findViewById(R.id.room_spinner);
            ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, R.layout.spinner_layout, rooms);
            dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(dataAdapter);
            builder.setView(dialogView);
            // Add confirm/cancel buttons
            builder.setPositiveButton("Add Room", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    RoomConfig roomConfig = new RoomConfig(dialogView.getContext(), spinner.getSelectedItem().toString());
                    roomConfig.update();
                    updateDisplay();
                }
            });
            // Add confirm/cancel buttons
            builder.setNegativeButton("Cancel", null);

            // Show the dialog
            AlertDialog dialog = builder.create();
            dialog.show();
        }
        else {
            // Build the dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("No room available");
            builder.setPositiveButton("Ok", null);

            // Show the dialog
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    public void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

}
