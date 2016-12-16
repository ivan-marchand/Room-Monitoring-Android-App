package marchandivan.RoomMonitoring;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v4.app.Fragment;
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
import marchandivan.RoomMonitoring.dialog.DeviceDialogBuilder;
import marchandivan.RoomMonitoring.dialog.SensorDialogBuilder;
import marchandivan.RoomMonitoring.fragment.DeviceConfigFragment;
import marchandivan.RoomMonitoring.fragment.SensorFragment;
import marchandivan.RoomMonitoring.receiver.MonitorRoomReceiver;

public class MainActivity extends AppCompatActivity {

    private void updateDisplay() {
        try {
            Log.d("MainActivity", "updateDisplay");
            // Add new room fragment
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            for (SensorConfig sensorConfig : SensorConfig.GetMap(this).values()) {
                String tag = "sensor_list_fragment_" + sensorConfig.getId() + "_" + sensorConfig.getName();
                Fragment existingFragment = fragmentManager.findFragmentByTag(tag);
                // Should be visible?
                if (sensorConfig.isVisible()) {
                    if (existingFragment == null) {
                        SensorFragment newFragment = new SensorFragment();
                        // Pass the name as parameter
                        Bundle args = new Bundle();
                        args.putLong("sensor_id", sensorConfig.getId());
                        newFragment.setArguments(args);
                        Log.d("updateDisplay", "Add fragment for " + tag);
                        fragmentTransaction.add(R.id.sensor_list_table, newFragment, tag);
                    }
                } else if (existingFragment != null) {
                    fragmentTransaction.remove(existingFragment);
                }
            }

            // Something to commit?
            if (!fragmentTransaction.isEmpty()) {
                fragmentTransaction.commit();
            }

            // Update temp/humidity display
            MonitorRoomReceiver.UpdateViews(getBaseContext());
        } catch (Exception e) {
            e.printStackTrace();
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

        // No sensor?
        if (SensorConfig.GetMap(this.getBaseContext()).isEmpty()) {
            final Activity activity = this;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.ask_add_device);
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    DeviceDialogBuilder deviceDialogBuilder = new DeviceDialogBuilder(activity);
                    deviceDialogBuilder.create().show();
                }
            });
            builder.setNegativeButton(R.string.no, null);
            builder.create().show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Update the display
        updateDisplay();

        // Get current room temp if not available
        if (!SensorConfig.GetMap(this.getBaseContext()).isEmpty() && SensorConfig.MeasureHasExpired(this.getBaseContext())) {
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
        } else if (id == R.id.add_sensor) {
            addDevice();
            return true;
        } else if (id == R.id.manage_sensors) {
            manageDevices();
        }

        return super.onOptionsItemSelected(item);
    }

    public void manageDevices() {
        Intent intent = new Intent(this, ManageDevicesActivity.class);
        startActivity(intent);
    }

    public void addDevice() {
        // Build and show dialog
        DeviceDialogBuilder deviceDialogBuilder = new DeviceDialogBuilder(this);
        deviceDialogBuilder.create().show();
    }

    public void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void addSensor(View view) {
        SensorDialogBuilder sensorDialogBuilder = new SensorDialogBuilder(this, new Runnable() {
            @Override
            public void run() {
                updateDisplay();
            }
        });

        // Create and show dialog
        AlertDialog alertDialog = sensorDialogBuilder.create();
        alertDialog.show();
    }
}
