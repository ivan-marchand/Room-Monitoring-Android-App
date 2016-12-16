package marchandivan.RoomMonitoring.sensor;

import android.content.Context;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import marchandivan.RoomMonitoring.db.DeviceConfig;
import marchandivan.RoomMonitoring.db.SensorConfig;
import marchandivan.RoomMonitoring.http.RestClient;

import static marchandivan.RoomMonitoring.db.SensorConfig.TypeFromString;

/**
 * Created by ivan on 10/24/16.
 */

public class GenericRestJsonApi extends Sensor {
    private String mBasePath;

    public GenericRestJsonApi() {
        super();
    }

    @Override
    public String getDisplayName() {
        return "OpenTemp Device";
    }

    public void setBasePath(String path) {
        mBasePath = path;
    }

    @Override
    public Sensor.AuthType getAuthType() {
        return Sensor.AuthType.USER_PASSWORD;
    }

    @Override
    public Sensor.AuthEncoding getAuthEncoding() {
        return Sensor.AuthEncoding.URL;
    }

    @Override
    public boolean needHostUrl() {
        return true;
    }

    @Override
    public boolean needBasePath() {
        return true;
    }

    @Override
    public boolean mandatoryCredential() {
        return false;
    }

    @Override
    public Sensor.ConnectionResult testConnection(Context context, DeviceConfig deviceConfig) {
        try {
            RestClient restClient = this.getRestClient(context, deviceConfig);
            restClient.setShowSslConfirmDialog(true);
            String basePath = mBasePath == null ? deviceConfig.getBasePath() : mBasePath;
            if (!basePath.endsWith("/")) {
                basePath += "/";
            }
            JSONObject result = restClient.getJson(basePath + "getTemperature/F");
            if (restClient.getHttpStatusCode() != HttpURLConnection.HTTP_OK) {
                return Sensor.ConnectionResult.UNABLE_TO_CONNECT;
            } else if (result != null && result.has("temperature")) {
                return Sensor.ConnectionResult.SUCCESS;
            } else {
                return Sensor.ConnectionResult.AUTH_FAILED;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Sensor.ConnectionResult.FAILURE;
        }
    }

    public void updateSensor(Context context, DeviceConfig deviceConfig, List<SensorConfig> sensorConfigs) {
        try {
            // Get Device
            RestClient restClient = getRestClient(context, deviceConfig);
            String basePath = mBasePath == null ? deviceConfig.getBasePath() : mBasePath;
            if (!basePath.endsWith("/")) {
                basePath += "/";
            }
            JSONObject reply  = restClient.getJson(basePath + "getThermostat");
            if (!sensorConfigs.isEmpty()) {
                SensorConfig sensorConfig = sensorConfigs.get(0);
                JSONObject data = new JSONObject();
                data.put("temperature", reply.get("temperature"));
                data.put("humidity", reply.get("humidity"));
                data.put("unit", reply.get("unit"));
                if (sensorConfig.getType() == SensorConfig.Type.THERMOSTAT) {
                    JSONObject thermostat = new JSONObject();
                    if (reply.has("temperatureCommand")) {
                        thermostat.put("temperature", reply.get("temperatureCommand"));
                    }
                    thermostat.put("mode", reply.get("mode"));
                    data.put("thermostat", thermostat);
                }
                sensorConfig.update(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean addSensors(Context context, final DeviceConfig deviceConfig) {
        try {
            RestClient restClient = getRestClient(context, deviceConfig);
            JSONObject sensorConfigJson = restClient.getJson("/getConfig");

            // Just add one sensor with the same name as the device
            JSONObject config = new JSONObject();
            SensorConfig.Type sensorType = TypeFromString(sensorConfigJson.getString("type"));
            SensorConfig sensorConfig = new SensorConfig(context, deviceConfig.getName(), sensorType, deviceConfig.getId(), new JSONObject());
            sensorConfig.add();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public ArrayList<Sensor.ConfigField> getConfigFields() {
        ArrayList<Sensor.ConfigField> fields = new ArrayList<>();
        fields.add(new Sensor.ConfigField("sensor", "Sensor Name", Sensor.ConfigFieldType.TEXT_LIST, true));
        return fields;
    }

    @Override
    protected void setThermostatImpl(Context context, DeviceConfig deviceConfig, SensorConfig sensorConfig, SensorConfig.ThermostatMode mode, Integer temperature) {
        try {
            RestClient restClient = getRestClient(context, deviceConfig);
            String basePath = mBasePath == null ? deviceConfig.getBasePath() : mBasePath;
            if (!basePath.endsWith("/")) {
                basePath += "/";
            }
            String path = basePath + "setThermostat/" + SensorConfig.ModeToString(mode);
            if (mode != SensorConfig.ThermostatMode.OFF && temperature != null) {
                path += "/" + temperature.toString();
            }
            JSONObject reply = restClient.getJson(path);
            JSONObject thermostat = new JSONObject();
            if (reply.has("temperatureCommand")) {
                thermostat.put("temperature", reply.get("temperatureCommand"));
            }
            if (reply.has("mode")) {
                thermostat.put("mode", reply.getString("mode"));
            }

            // Update sensor
            JSONObject data = sensorConfig.getData();
            data.put("thermostat", thermostat);
            sensorConfig.update(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
