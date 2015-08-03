package marchandivan.babyroommonitoring;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private Handler mHandler;
    private int mInterval = 20000;
    //private AlarmManager mAlarmManager;
    private RestClient mRestClient;

    protected class UpdateRoom extends AsyncTask<Void, Void, JSONObject> {

        protected JSONObject doInBackground(Void...voids) {
            return mRestClient.get();
        }

        protected void onPostExecute(JSONObject result) {
            try {
                if (result.has("temperature")) {
                    updateDisplay(result);
                    /*AlarmManager.ComparisonResult comparisonResult = mAlarmManager.compare(Integer.parseInt(result.getString("temperature")));
                    if (comparisonResult != AlarmManager.ComparisonResult.WITHIN) {
                        Log.d("UpdateRoom", "Start Alarm");
                        Intent intent = new Intent(getThis(), Alarm.class);
                        startActivity(intent);
                    }
                    else {
                        mAlarmManager.stop();
                    }*/

                }
                else {
                    toast("Unable to retrieve room temperature");
                }
            }
            catch (Exception e) {
                Log.d("UpdateRoom", "Error " + e.toString());
            }
        }
    }

    private Activity getThis() {
        return this;
    }

    private void toast(String message) {
        Toast toast = Toast.makeText(getThis(), message, Toast.LENGTH_SHORT);
        toast.show();
    }

    private void updateDisplay(JSONObject result) {
        try {
        // Update the text view
        TextView textView = (TextView) findViewById(R.id.room_temperature);
        textView.setText(result.getString("temperature") + " F");
        }
        catch (Exception e) {
            Log.d("UpdateDisplay", "Error " + e.toString());
        }
    }

    private Runnable mRoomChecker = new Runnable() {
        @Override
        public void run() {
            try {
                new UpdateRoom().execute();
            } finally {
                mHandler.postDelayed(mRoomChecker, mInterval);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new Handler();
        mRestClient = new RestClient();
        //mAlarmManager = new AlarmManager(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Get host, port from settings
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        //mAlarmManager.configure(sharedPref);
        mRestClient.configure(sharedPref);
        mRoomChecker.run();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    public void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
}
