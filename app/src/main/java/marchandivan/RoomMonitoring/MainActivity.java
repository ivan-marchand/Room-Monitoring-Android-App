package marchandivan.RoomMonitoring;

import android.app.Activity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import marchandivan.RoomMonitoring.db.SensorConfig;
import marchandivan.RoomMonitoring.dialog.SensorDialogBuilder;
import marchandivan.RoomMonitoring.fragment.SensorFragment;
import marchandivan.RoomMonitoring.receiver.MonitorRoomReceiver;

public class MainActivity extends AppCompatActivity {

    private void updateDisplay() {
        try {
            // Add new room fragment
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            for (SensorConfig sensorConfig : SensorConfig.GetMap(this).values()) {
                // Should be visible?
                SensorFragment fragment = new SensorFragment();
                // Pass the name as parameter
                Bundle args = new Bundle();
                args.putLong("sensor_id", sensorConfig.getId());
                fragment.setArguments(args);
                // Add fragment to main activity
                String aTag = "sensor_list_fragment_" + sensorConfig.getId() + "_" + sensorConfig.getName();
                if (fragmentManager.findFragmentByTag(aTag) == null) {
                    Log.d("updateDisplay", "Add fragment for " + aTag);
                    fragmentTransaction.add(R.id.sensor_list_table, fragment, aTag);
                }
            }

            // Something to commit?
            if (!fragmentTransaction.isEmpty()) {
                fragmentTransaction.commit();
            }

            // Update temp/humidity display
            MonitorRoomReceiver.UpdateViews(getBaseContext());
        }
        catch (Exception e) {

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("MainActivity", "onCreate");

        // Set the layout
        setContentView(marchandivan.RoomMonitoring.R.layout.activity_main);

        // Activate the room monitoring
        MonitorRoomReceiver.Activate(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Update the display
        updateDisplay();

        // Get current room temp if not available
        if (SensorConfig.MeasureHasExpired(this.getBaseContext())) {
            final Activity activity = this;
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    // Show and animate progress bar
                    final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.VISIBLE);
                            progressBar.bringToFront();
                            progressBar.animate();
                        }
                    });
                    // Get current temperature
                    MonitorRoomReceiver.Update(activity);
                    // Stop and hide progress bar
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.animate().cancel();
                            progressBar.setVisibility(View.GONE);
                        }
                    });
                }
            });
            thread.start();
        }
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

    public void addSensor(View view) {
        // Build and show dialog
        SensorDialogBuilder addSensorDialogBuilder = new SensorDialogBuilder(this);
        addSensorDialogBuilder.show();
    }

    public void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

}
