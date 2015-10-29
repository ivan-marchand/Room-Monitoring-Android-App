package marchandivan.RoomMonitoring.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by ivan on 10/26/15.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Activate alarm at boot if monitoring is active
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            if (sharedPreferences.getBoolean("temperature_alarm", false)) {
                Log.d("BootReceiver", "Activate monitoring");
                MonitorRoomReceiver.Activate(context);
            }
        }
    }

}
