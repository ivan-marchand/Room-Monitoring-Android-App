package marchandivan.babyroommonitoring;

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

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private Handler mHandler;
    private int mInterval = 20000;
    private String mYunHost;
    private String mYunPort;

    protected class UpdateRoom extends AsyncTask<String, Void, JSONObject> {

        protected JSONObject doInBackground(String... urls) {
            RestClient restClient = new RestClient();
            return restClient.get(urls[0]);
        }

        protected void onPostExecute(JSONObject result) {
            try {
                // Update the text view
                TextView textView = (TextView) findViewById(R.id.room_temperature);
                textView.setText(result.getString("value") + " F");
            }
            catch (Exception e) {
                Log.d("UpdateRoom", "Error " + e.getMessage());
            }
        }
    }

    private Runnable mRoomChecker = new Runnable() {
        @Override
        public void run() {
            new UpdateRoom().execute("http://" + mYunHost + ":" + mYunPort + "/data/get/temp");
            mHandler.postDelayed(mRoomChecker, mInterval);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new Handler();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Get host, port from settings
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mYunHost = sharedPref.getString("yun_host", "");
        mYunPort = sharedPref.getString("yun_port", "");
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
