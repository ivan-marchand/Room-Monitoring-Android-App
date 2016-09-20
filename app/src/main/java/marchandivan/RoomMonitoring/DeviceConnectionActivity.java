package marchandivan.RoomMonitoring;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.support.v7.app.AppCompatActivity;

import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import marchandivan.RoomMonitoring.db.DeviceConfig;
import marchandivan.RoomMonitoring.sensor.Sensor;
import marchandivan.RoomMonitoring.sensor.SensorFactory;


/**
 * A login screen that offers login via user/password.
 */
public class DeviceConnectionActivity extends AppCompatActivity {

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private ConnectionTask mConnectionTask = null;

    private Sensor mSensor = null;

    // UI references.
    private AutoCompleteTextView mDeviceNameView;
    private CheckBox mUseHttpsView;
    private AutoCompleteTextView mHostView;
    private AutoCompleteTextView mPortView;
    private AutoCompleteTextView mUserView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mConnectionFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_connection);

        // Get sensor
        mSensor = SensorFactory.Get(getIntent().getStringExtra("sensorType"));

        // Activity title
        setTitle(mSensor.getDisplayName());

        // Set icon
        if (mSensor.getIcon() != 0) {
            ImageView icon = (ImageView) findViewById(R.id.sensor_icon);
            ImageView progressIcon = (ImageView) findViewById(R.id.progress_bar_icon);
            progressIcon.setImageResource(mSensor.getIcon());
            icon.setImageResource(mSensor.getIcon());
        }

        // Device name
        mDeviceNameView = (AutoCompleteTextView) findViewById(R.id.device_name);
        mDeviceNameView.setText(mSensor.getDisplayName());

        // Set URL form.
        if (mSensor.needHostUrl()) {
            findViewById(R.id.url_form).setVisibility(View.VISIBLE);
            mUseHttpsView = (CheckBox) findViewById(R.id.use_https);
            mHostView = (AutoCompleteTextView) findViewById(R.id.host);
            mPortView = (AutoCompleteTextView) findViewById(R.id.port);
        }

        switch (mSensor.getAuthType()) {
            case USER_PASSWORD:
                findViewById(R.id.user_password_form).setVisibility(View.VISIBLE);
                mUserView = (AutoCompleteTextView) findViewById(R.id.user);
                mUserView.setText("marchandivan@gmail.com");
                // Force password re-entry in case of modification
                mPasswordView = (EditText) findViewById(R.id.password);
                //mPasswordView.setText("");
                mPasswordView.setText("stadiumIM01");
                mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                        if (id == R.id.login || id == EditorInfo.IME_NULL) {
                            attemptConnection();
                            return true;
                        }
                        return false;
                    }
                });
                break;

            case NONE:
            default:
                break;
        }

        Button mUserSignInButton = (Button) findViewById(R.id.connect_button);
        mUserSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptConnection();
            }
        });

        mConnectionFormView = findViewById(R.id.connection_form);
        mProgressView = findViewById(R.id.connection_progress);
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid user, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptConnection() {
        if (mConnectionTask != null) {
            return;
        }

        // Reset errors.
        mDeviceNameView.setError(null);
        if (mHostView != null) {
            mHostView.setError(null);
        }
        if (mPortView != null) {
            mPortView.setError(null);
        }
        if (mUserView != null) {
            mUserView.setError(null);
        }
        if (mPasswordView != null) {
            mPasswordView.setError(null);
        }

        // Store values at the time of the login attempt.
        String deviceName = mDeviceNameView.getText().toString();
        boolean useHttps = mUseHttpsView != null ? mUseHttpsView.isChecked() : false;
        String host = mHostView != null ? mHostView.getText().toString() : "";
        int port = mPortView != null ? Integer.valueOf(mPortView.getText().toString()) : 0;
        String user = mUserView != null ? mUserView.getText().toString() : "";
        String password = mPasswordView != null ? mPasswordView.getText().toString() : "";

        boolean cancel = false;
        View focusView = null;

        if (TextUtils.isEmpty(deviceName)) {
            mDeviceNameView.setError(getString(R.string.error_field_required));
            focusView = mDeviceNameView;
            cancel = true;
        }

        // Check for a valid host, port, user
        if (!cancel) {
            if (mSensor.needHostUrl()) {
                if (TextUtils.isEmpty(host)) {
                    mHostView.setError(getString(R.string.error_field_required));
                    focusView = mHostView;
                    cancel = true;
                } else if (port == 0) {
                    mPortView.setError(getString(R.string.error_field_required));
                    focusView = mPortView;
                    cancel = true;
                }
            }
        }

        if (!cancel) {
            switch (mSensor.getAuthType()) {
                case USER_PASSWORD:
                    if (TextUtils.isEmpty(user)) {
                        mUserView.setError(getString(R.string.error_field_required));
                        focusView = mUserView;
                        cancel = true;
                    } else if (TextUtils.isEmpty(password)) {
                        mPasswordView.setError(getString(R.string.error_field_required));
                        focusView = mPasswordView;
                        cancel = true;
                    }
                    break;
                case NONE:
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
            mConnectionTask = new ConnectionTask(deviceName, useHttps, host, port, user, password);
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

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class ConnectionTask extends AsyncTask<Void, Void, Sensor.ConnectionResult> {

        private DeviceConfig mDeviceConfig;

        ConnectionTask(String deviceName, boolean https, String host, int port, String user, String password) {
            if (mSensor.needHostUrl()) {
                mDeviceConfig = new DeviceConfig(getBaseContext(), deviceName, mSensor.getDeviceType(), https, host, port);
            } else {
                mDeviceConfig = new DeviceConfig(getBaseContext(), deviceName, mSensor.getDeviceType());
            }

            switch (mSensor.getAuthType()) {
                case USER_PASSWORD:
                    mDeviceConfig.setUserPassword(user, password);
            }
        }

        @Override
        protected Sensor.ConnectionResult doInBackground(Void... params) {

            try {
                return mSensor.testConnection(getBaseContext(), mDeviceConfig);
            } catch (Exception e) {
                return Sensor.ConnectionResult.FAILURE;
            }
        }

        @Override
        protected void onPostExecute(final Sensor.ConnectionResult result) {
            mConnectionTask = null;
            showProgress(false);

            if (result == Sensor.ConnectionResult.SUCCESS) {
                Toast.makeText(getBaseContext(), R.string.connection_successful, Toast.LENGTH_LONG).show();
                // Store device
                mDeviceConfig.add();

                // Exit activity
                finish();
            } else {
                Toast.makeText(getBaseContext(), R.string.connection_failed, Toast.LENGTH_LONG).show();
                if (mSensor.needHostUrl() && result == Sensor.ConnectionResult.UNABLE_TO_CONNECT) {
                    mHostView.setError(getString(R.string.error_unable_to_connect));
                    mPortView.setError(getString(R.string.error_unable_to_connect));
                    mHostView.requestFocus();
                } else {
                    switch (mSensor.getAuthType()) {
                        case USER_PASSWORD:
                            mUserView.setError(getString(R.string.error_invalid_user_password));
                            mPasswordView.setError(getString(R.string.error_invalid_user_password));
                            mUserView.requestFocus();
                            break;
                        case NONE:
                        default:
                            break;
                    }
                }
            }
        }

        @Override
        protected void onCancelled() {
            mConnectionTask = null;
            showProgress(false);
        }
    }
}

