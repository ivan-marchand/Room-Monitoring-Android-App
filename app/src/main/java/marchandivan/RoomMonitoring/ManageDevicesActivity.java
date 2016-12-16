package marchandivan.RoomMonitoring;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import marchandivan.RoomMonitoring.db.DeviceConfig;
import marchandivan.RoomMonitoring.fragment.DeviceConfigFragment;
import marchandivan.RoomMonitoring.sensor.Sensor;
import marchandivan.RoomMonitoring.sensor.SensorFactory;

public class ManageDevicesActivity extends AppCompatActivity {

    private void updateDisplay() {
        try {
            Log.d("ManageDevicesActivity", "updateDisplay");
            // Add new room fragment
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            for (DeviceConfig deviceConfig: DeviceConfig.GetMap(this).values()) {
                // Should be visible?
                DeviceConfigFragment fragment = DeviceConfigFragment.newInstance(deviceConfig.getId());
                // Add fragment to main activity
                String aTag = "device_list_fragment_" + deviceConfig.getId() + "_" + deviceConfig.getName();
                if (fragmentManager.findFragmentByTag(aTag) == null) {
                    Log.d("updateDisplay", "Add fragment for " + aTag);
                    fragmentTransaction.add(R.id.activity_manage_devices, fragment, aTag);
                }
            }

            // Something to commit?
            if (!fragmentTransaction.isEmpty()) {
                fragmentTransaction.commit();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_devices);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Update display
        updateDisplay();
    }
}
