package marchandivan.babyroommonitoring;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

/**
 * Created by imarchand on 7/3/2015.
 */
public class AlarmManager {
    private boolean mAlarmActivated = false;
    private Integer mMaxTemp = null;
    private Integer mMinTemp = null;
    private Ringtone mRingtone;
    private Context mContext;

    public enum ComparisonResult{
        BELOW,
        WITHIN,
        ABOVE
    }
    public AlarmManager(Context context) {
        mAlarmActivated = false;
        mMaxTemp = new Integer(0);
        mMinTemp = new Integer(0);
        mContext = context;
        mRingtone = RingtoneManager.getRingtone(mContext, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
    }

    public void configure(SharedPreferences sharedPreferences) {
        mAlarmActivated = sharedPreferences.getBoolean("temperature_alarm", false);
        if (mAlarmActivated) {
            // Get min and max temp
            mMaxTemp = Integer.parseInt(sharedPreferences.getString("alarm_max_value", "74"));
            mMinTemp = Integer.parseInt(sharedPreferences.getString("alarm_min_value", "70"));
            Log.d("AlarmManager", "MaxTemp : " + mMaxTemp.toString() + " MinTemp : " + mMinTemp.toString());

            // Stop alarm if already ringing
            this.stop();

            // Get alarm Ringtone
            Uri ringtoneUri = Uri.parse(sharedPreferences.getString("alarm_ringtone", ""));
            if (ringtoneUri != null) {
                mRingtone = RingtoneManager.getRingtone(mContext, ringtoneUri);
            }
        }
    }

    public void play() {
        //mRingtone.play();
    }

    public void stop() {
        if (mRingtone.isPlaying()) {
            //mRingtone.stop();
        }
    }
    public ComparisonResult compare(int temp) {
        if (mAlarmActivated) {
            if (mMaxTemp != null && temp > mMaxTemp) {
                return ComparisonResult.ABOVE;
            }
            else if (mMinTemp != null && temp < mMinTemp) {
                return ComparisonResult.BELOW;
            }
            else {
                return ComparisonResult.WITHIN;
            }
        }
        return ComparisonResult.WITHIN;
    }
}

