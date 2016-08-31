package marchandivan.RoomMonitoring;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONException;


import java.util.HashMap;

import marchandivan.RoomMonitoring.db.RoomConfig;
import marchandivan.RoomMonitoring.receiver.MonitorRoomReceiver;


public class AlarmActivity extends Activity {
    private Ringtone mRingtone;
    private Boolean mAlarmVibrate = false;
    private Integer mSilenceAfter;
    private String mRoomName = "";
    private Float mTemperature = new Float(0);
    private Integer mMinTemperature = 0;
    private Integer mMaxTemperature = 0;

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

        // Get room name
        mRoomName = getIntent().getExtras().getString("room");
        mMaxTemperature = getIntent().getExtras().getInt("max_temperature");
        mMinTemperature = getIntent().getExtras().getInt("min_temperature");
        TextView roomTextView = (TextView)findViewById(R.id.temperature_alert_room);
        char[] roomCapitalized = mRoomName.toCharArray();
        roomCapitalized[0] = Character.toUpperCase(roomCapitalized[0]);
        roomTextView.setText(new String(roomCapitalized));

        // Update Temperature display
        HashMap<String, RoomConfig> roomConfigs = RoomConfig.GetMap(this);
        try {
            mTemperature = Float.parseFloat(roomConfigs.get(mRoomName).mData.getString("temperature"));
            TextView temperatureTextView = (TextView)findViewById(R.id.temperature_alert_temperature);
            temperatureTextView.setText(String.format("%.1f F", mTemperature));
        } catch (JSONException e) {
            // Ignore
        }

        // Update background color and alert icon
        if (mTemperature > mMaxTemperature) {
            FrameLayout alarmScreen = (FrameLayout)findViewById(R.id.alarm_screen);
            alarmScreen.setBackgroundColor(getResources().getColor(R.color.orange_red));
            ImageView alertIcon = (ImageView)findViewById(R.id.temperature_alert_icon);
            alertIcon.setImageResource(R.drawable.hot_64);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Configure alarm
        this.configure();

        // Play ring tone
        this.playRingtone();

        // Silence after ?
        if (mSilenceAfter != 0) {
            final Runnable stopAlarm = new Runnable() {
                @Override
                public void run() {
                    stopRingtone();
                }
            };
            Handler handler = new Handler();
            handler.postDelayed(stopAlarm, mSilenceAfter * 60 * 1000);
        }
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
        // Play ringtone if defined
        if (mRingtone != null) {
            Log.d("Alarm", "Playing !");
            mRingtone.play();
        }

        // Make the phone vibrate
        if (mAlarmVibrate) {
            Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
            long[] pattern = {500, 500};
            vibrator.vibrate(pattern, 0);
        }
    }

    public void stopAlarm(View view) {
        this.stopRingtone();
        NavUtils.navigateUpFromSameTask(this);
    }

    public void stopRingtone() {
        // Stop ringtone
        if (mRingtone != null && mRingtone.isPlaying()) {
            mRingtone.stop();
        }
        // Stop vibration
        Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.cancel();
    }

    public void configure() {

        // Deactivate alarm if already ringing
        this.stopRingtone();

        // Get alarm Ringtone
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Uri ringtoneUri = Uri.parse(sharedPreferences.getString("alarm_ringtone", ""));
        if (ringtoneUri != null && mRingtone != null) {
            mRingtone = RingtoneManager.getRingtone(this, ringtoneUri);
            mRingtone.setStreamType(AudioManager.STREAM_ALARM);
            Log.d("Alarm", ringtoneUri.toString());
        }

        // Alarm vibrate ?
        mAlarmVibrate = sharedPreferences.getBoolean("alarm_vibrate", false);

        // Silence after
        mSilenceAfter = Integer.parseInt(sharedPreferences.getString("alarm_silence_after", "0"));
    }
}