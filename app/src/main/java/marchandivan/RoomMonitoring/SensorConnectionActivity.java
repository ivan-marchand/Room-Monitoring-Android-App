package marchandivan.RoomMonitoring;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import marchandivan.RoomMonitoring.db.DeviceConfig;
import marchandivan.RoomMonitoring.db.SensorConfig;
import marchandivan.RoomMonitoring.receiver.MonitorRoomReceiver;
import marchandivan.RoomMonitoring.sensor.Sensor;
import marchandivan.RoomMonitoring.sensor.SensorFactory;

public class SensorConnectionActivity extends AppCompatActivity {
    private ConnectionTask mConnectionTask = null;

    // UI references.
    private AutoCompleteTextView mSensorNameView;

    private DeviceConfig mDeviceConfig;
    private Sensor mSensor;
    private HashMap<Sensor.ConfigField, View> mCustomFieldViewMap = new HashMap<>();
    private View mProgressView;
    private View mConnectionFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_connection);

        // Get Device config
        long sensorId = getIntent().getLongExtra("sensorId", 0);
        Log.d("SensorConnection", String.valueOf(sensorId));
        mDeviceConfig = new DeviceConfig(getBaseContext(), sensorId);
        mDeviceConfig.read();
        mSensor = SensorFactory.Get(mDeviceConfig.getType());

        // Set device icon
        if (mSensor.getIcon() != 0) {
            ImageView icon = (ImageView) findViewById(R.id.sensor_icon);
            ImageView progressIcon = (ImageView) findViewById(R.id.progress_bar_icon);
            progressIcon.setImageResource(mSensor.getIcon());
            icon.setImageResource(mSensor.getIcon());
        }

        // Sensor name
        mSensorNameView = (AutoCompleteTextView) findViewById(R.id.sensor_name);

        // Add custom fields
        LinearLayout customFieldsView = (LinearLayout) findViewById(R.id.custom_fields);
        for(Sensor.ConfigField configField: mSensor.getConfigFields()) {
            customFieldsView.addView(createCustomFieldView(configField));
        }

        // Save button
        Button saveButton = (Button) findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptConnection();
            }
        });

        // Progress bar
        mConnectionFormView = findViewById(R.id.connection_form);
        mProgressView = findViewById(R.id.connection_progress);

    }

    private View createCustomFieldView(Sensor.ConfigField configField) {
        switch (configField.getType()) {
            case NUMBER:
            case TEXT:
                View fieldTextView = getLayoutInflater().inflate(R.layout.field_text, null);
                AutoCompleteTextView textView = (AutoCompleteTextView) fieldTextView.findViewById(R.id.text_view);
                textView.setHint(configField.getDisplayName());
                if (configField.getType() == Sensor.ConfigFieldType.NUMBER) {
                    textView.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                }
                mCustomFieldViewMap.put(configField, textView);
                return fieldTextView;
            case TEXT_LIST:
                View fieldTextListView = getLayoutInflater().inflate(R.layout.field_text_list, null);
                TextView promptView = (TextView) fieldTextListView.findViewById(R.id.prompt_view);
                promptView.setText(configField.getDisplayName());
                Spinner spinner = (Spinner) fieldTextListView.findViewById(R.id.text_list_view);
                mCustomFieldViewMap.put(configField, spinner);
                return fieldTextListView;

            default:
                return null;
        }
    }

    private void attemptConnection() {
        if (mConnectionTask != null) {
            return;
        }

        boolean cancel = false;
        View focusView = null;

        // Reset errors.
        mSensorNameView.setError(null);
        // Store values at the time of the login attempt.
        String sensorName = mSensorNameView.getText().toString();
        if (TextUtils.isEmpty(sensorName)) {
            mSensorNameView.setError(getString(R.string.error_field_required));
            focusView = mSensorNameView;
            cancel = true;
        }

        // Config
        JSONObject config = new JSONObject();
        for (Map.Entry<Sensor.ConfigField, View> entry: mCustomFieldViewMap.entrySet()) {
            switch (entry.getKey().getType()) {
                case NUMBER:
                case TEXT:
                    AutoCompleteTextView textView = (AutoCompleteTextView) entry.getValue();
                    textView.setError(null);

                    // Check value
                    String value = textView.getText().toString();
                    if (TextUtils.isEmpty(value)) {
                        if (entry.getKey().isMandatory()) {
                            textView.setError(getString(R.string.error_field_required));
                            focusView = textView;
                            cancel = true;
                        }
                    } else {
                        try {
                            if (entry.getKey().getType() == Sensor.ConfigFieldType.NUMBER) {
                                config.put(entry.getKey().getKey(), Integer.valueOf(value));
                            } else {
                                config.put(entry.getKey().getKey(), value);
                            }
                        } catch (Exception e) {
                            // Ignore
                        }
                    }
                    break;
                case TEXT_LIST:
                    Spinner spinner = (Spinner) entry.getValue();
                    try {
                        config.put(entry.getKey().getKey(), spinner.getSelectedItem().toString());
                    } catch (Exception e) {
                        // Ignore
                    }
                default:
                    break;
            }
        }



        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mConnectionTask = new ConnectionTask(sensorName, config);
            mConnectionTask.execute((Void) null);
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mConnectionFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mConnectionFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mConnectionFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mConnectionFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    public class ConnectionTask extends AsyncTask<Void, Void, JSONObject> {
        SensorConfig mSensorConfig;

        public ConnectionTask(String sensorName, JSONObject config) {
            //mSensorConfig = new SensorConfig(getBaseContext(), sensorName, mSensor.getType(), mDeviceConfig.getId(), config);
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            JSONObject result = new JSONObject();
            try {
                //result =  mSensor.getSensorMeasure(getBaseContext(), mSensorConfig);
            } catch (Exception e) {
            }
            return  result;
        }

        @Override
        protected void onPostExecute(final JSONObject result) {
            mConnectionTask = null;
            showProgress(false);

            // Does json contains temperature value?
            boolean validJson = false;
            try {
                 validJson = result.has("temperature") && result.get("temperature") instanceof Double;
            } catch (Exception e) {
                validJson = false;
            }

            if (validJson) {
                Toast.makeText(getBaseContext(), R.string.connection_successful, Toast.LENGTH_LONG).show();
                // Store sensor
                mSensorConfig.setData(result);
                mSensorConfig.add();

                // Exit activity
                finish();
            } else {
                Toast.makeText(getBaseContext(), R.string.connection_failed, Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onCancelled() {
            mConnectionTask = null;
            showProgress(false);
        }

    }
}
