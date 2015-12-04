package marchandivan.RoomMonitoring;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;
import android.view.Window;
import android.view.WindowManager;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class AlarmActivity extends Activity {
    private Ringtone mRingtone;
    private Integer mSilenceAfter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(marchandivan.RoomMonitoring.R.layout.activity_alarm);
        setupActionBar();

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Configure alarm
        this.configure();

        // Play ring tone
        this.playRingtone();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Stop ring tone
        this.stopRingtone();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar.
            getActionBar().hide();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            // that hierarchy.
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void playRingtone() {
        if (mRingtone != null) {
            Log.d("Alarm", "Playing !");
            mRingtone.play();
        }
    }

    public void stopAlarm(View view) {
        this.stopRingtone();
        NavUtils.navigateUpFromSameTask(this);
    }

    public void stopRingtone() {
        if (mRingtone != null && mRingtone.isPlaying()) {
            mRingtone.stop();
        }
    }

    public void configure() {

        // Deactivate alarm if already ringing
        this.stopRingtone();

        // Get alarm Ringtone
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Uri ringtoneUri = Uri.parse(sharedPreferences.getString("alarm_ringtone", ""));
        if (ringtoneUri != null) {
            mRingtone = RingtoneManager.getRingtone(this, ringtoneUri);
            mRingtone.setStreamType(AudioManager.STREAM_ALARM);
            Log.d("Alarm", ringtoneUri.toString());
        }
    }
}