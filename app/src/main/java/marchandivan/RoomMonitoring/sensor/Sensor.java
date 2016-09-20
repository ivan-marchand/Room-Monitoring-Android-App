package marchandivan.RoomMonitoring.sensor;

import android.content.Context;
import android.os.AsyncTask;

import org.json.JSONObject;

import java.net.CookieManager;
import java.util.ArrayList;
import java.util.HashMap;

import marchandivan.RoomMonitoring.db.DeviceConfig;
import marchandivan.RoomMonitoring.db.SensorConfig;
import marchandivan.RoomMonitoring.http.RestClient;

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

    public enum Type {
        THERMOMETER, THERMOSTAT
    }

    public static String ToString(Type type) {
        switch (type) {
            case THERMOMETER:
                return "thermometer";
            case THERMOSTAT:
                return "thermostat";
        }
        return "";
    }

    public static Type FromString(final String string) {
        switch (string) {
            case "thermostat":
                return Type.THERMOSTAT;
            case "thermometer":
                return Type.THERMOMETER;
        }
        return Type.THERMOMETER;
    }

    private String mDeviceType;
    private Type mType;

    public Sensor(final Type type) {
        mDeviceType = this.getClass().getSimpleName();
        mType = type;
    }

    public Type getType() {
        return mType;
    }

    public final String getDeviceType() {
        return mDeviceType;
    }

    public String getDisplayName()
    {
        return mDeviceType;
    }

    public enum AuthType {
        NONE, USER_PASSWORD, TOKEN
    }

    // Get Authentication type
    public abstract AuthType getAuthType();

    // Sensor need Host URL config?
    public abstract boolean needHostUrl();

    // Icon
    public int getIcon() {
        return 0;
    }

    // Login
    public enum ConnectionResult {
        SUCCESS, FAILURE, UNABLE_TO_CONNECT, AUTH_FAILED
    }

    public RestClient getRestClient(Context context, final DeviceConfig deviceConfig) {
        RestClient restClient = new RestClient(context, deviceConfig.isHttps(), deviceConfig.getHost(), deviceConfig.getPort());
        if (this.getAuthType() == AuthType.USER_PASSWORD && deviceConfig.getUser() != null && deviceConfig.getPassword() != null) {
            restClient.setUserPassword(deviceConfig.getUser(), deviceConfig.getPassword());
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

    public abstract JSONObject getSensorMeasure(Context context, SensorConfig sensorConfig);

    public abstract ArrayList<ConfigField> getConfigFields();

    // Get possible value of a field for autocompletion
    public abstract ArrayList<String> getTextFieldValuesImpl(Context context, final DeviceConfig deviceConfig, final String key);
    public ArrayList<String> getTextFieldValues(final Context context, final DeviceConfig deviceConfig, final String key) {
        AsyncTask<Void, Void, ArrayList<String>> asyncTask = new AsyncTask<Void, Void, ArrayList<String>>() {
            @Override
            protected ArrayList<String> doInBackground(Void... params) {
                return getTextFieldValuesImpl(context, deviceConfig, key);
            }
        };
        asyncTask.execute();
        try {
            return asyncTask.get();
        } catch (Exception e) {
            return null;
        }
    }
}
