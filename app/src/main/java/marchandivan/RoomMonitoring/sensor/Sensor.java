package marchandivan.RoomMonitoring.sensor;

import android.content.Context;
import android.os.AsyncTask;

import org.json.JSONObject;

import java.net.CookieManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import marchandivan.RoomMonitoring.R;
import marchandivan.RoomMonitoring.db.DeviceConfig;
import marchandivan.RoomMonitoring.db.SensorConfig;
import marchandivan.RoomMonitoring.http.RestClient;

import static marchandivan.RoomMonitoring.db.SensorConfig.ModeToString;

/**
 * Created by ivan on 9/5/16.
 */
public abstract class Sensor {

    public enum ConfigFieldType {
        TEXT, TEXT_LIST, NUMBER, BOOL
    }

    public class ConfigField {
        private String mKey;
        private String mDisplayName;
        private Boolean mMandatory;
        private ConfigFieldType mType;

        public ConfigField(final String key, final String displayName, final ConfigFieldType type, boolean mandatory) {
            mKey = key;
            mDisplayName = displayName;
            mType = type;
            mMandatory = mandatory;
        }

        public final String getKey() {
            return mKey;
        }

        public final String getDisplayName() {
            return mDisplayName;
        }

        public final ConfigFieldType getType() {
            return mType;
        }

        public boolean isMandatory() {
            return mMandatory;
        }
    }

    private static HashMap<Long, CookieManager> msCookieManagers = new HashMap<>();

    public static CookieManager GetCookieManager(DeviceConfig deviceConfig) {
        if (!msCookieManagers.containsKey(deviceConfig.getId())) {
            msCookieManagers.put(deviceConfig.getId(), new CookieManager());
        }
        return msCookieManagers.get(deviceConfig.getId());
    }

    private String mDeviceType;

    public Sensor() {
        mDeviceType = this.getClass().getSimpleName();
    }

    public final String getDeviceType() {
        return mDeviceType;
    }

    public String getDisplayName()
    {
        return mDeviceType;
    }

    // Get Authentication type
    public enum AuthType {
        NONE, USER_PASSWORD, TOKEN
    }
    public abstract AuthType getAuthType();

    // Get Authentication url
    public enum AuthEncoding {
        NONE, HEADER, URL
    }
    public abstract AuthEncoding getAuthEncoding();

    // Sensor need Host URL config?
    public abstract boolean needHostUrl();

    // Sensor need URL Base Path config?
    public boolean needBasePath() {
        return false;
    }

    // Are Credential Mandatory?
    public boolean mandatoryCredential() {
        return true;
    }

    // Icon
    public int getIcon() {
        return R.drawable.thermometer;
    }

    // Login
    public enum ConnectionResult {
        SUCCESS, FAILURE, UNABLE_TO_CONNECT, AUTH_FAILED
    }

    public RestClient getRestClient(Context context, final DeviceConfig deviceConfig) {
        RestClient restClient = new RestClient(context, deviceConfig.isHttps(), deviceConfig.getHost(), deviceConfig.getPort());
        if (this.getAuthType() == AuthType.USER_PASSWORD && deviceConfig.getUser() != null && deviceConfig.getPassword() != null) {
            switch (this.getAuthEncoding()) {
                case HEADER:
                    restClient.setUserPassword(deviceConfig.getUser(), deviceConfig.getPassword());
                    break;
                case URL:
                    if (deviceConfig.getUser() != null && deviceConfig.getPassword() != null) {
                        restClient.addUrlParam("username", deviceConfig.getUser());
                        restClient.addUrlParam("password", deviceConfig.getPassword());
                    }
                    break;
                default:
                    break;
            }
        }
        return restClient;
    }

    public RestClient getRestClient(Context context, final SensorConfig sensorConfig) {
        // Get associated device config
        DeviceConfig deviceConfig = new DeviceConfig(context, sensorConfig.getDeviceId());
        deviceConfig.read();
        return this.getRestClient(context, deviceConfig);
    }

    public ConnectionResult testConnection(Context context, final DeviceConfig deviceConfig) {
        return needHostUrl() ? ConnectionResult.FAILURE : ConnectionResult.SUCCESS;
    }

    public abstract boolean addSensors(Context context, final DeviceConfig deviceConfig);

    public boolean save(final Context context, final DeviceConfig deviceConfig) {
        boolean result = false;
        // Store device config in Db
        deviceConfig.add();

        AsyncTask<Void, Void, Boolean> addSensorsTask = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                return addSensors(context, deviceConfig);
            }
        };
        try {
            addSensorsTask.execute();
            if (addSensorsTask.get()) {
                result = true;
            } else {
                // In case of error, roll back
                deviceConfig.delete();
            }
        } catch (Exception e) {
            // In case of error, roll back device config
            deviceConfig.delete();
        }
        return result;
    }

    public abstract void updateSensor(Context context, DeviceConfig deviceConfig, List<SensorConfig> sensorConfigs);

    protected void setThermostatImpl(Context context, DeviceConfig deviceConfig, SensorConfig sensorConfig, SensorConfig.ThermostatMode mode, Integer temperature) {
    }

    public void setThermostat(final Context context,
                              final DeviceConfig deviceConfig,
                              final SensorConfig sensorConfig,
                              final SensorConfig.ThermostatMode mode,
                              final Integer temperature,
                              final Runnable updateDisplayTask) {
        try {
            AsyncTask<Void, Void, Void> setThermostatTask = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    setThermostatImpl(context, deviceConfig, sensorConfig, mode, temperature);
                    return null;
                }
            };

            // Run the thread
            setThermostatTask.execute();

            // Update sensor cache
            JSONObject data = sensorConfig.getData();
            JSONObject thermostat = data.has("thermostat") ? data.getJSONObject("thermostat") : new JSONObject();
            thermostat.put("mode", ModeToString(mode));
            if (temperature != null) {
                thermostat.put("temperature", temperature);
            }
            data.put("thermostat", thermostat);
            sensorConfig.update(data);

            // Update display
            updateDisplayTask.run();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public abstract ArrayList<ConfigField> getConfigFields();

}
