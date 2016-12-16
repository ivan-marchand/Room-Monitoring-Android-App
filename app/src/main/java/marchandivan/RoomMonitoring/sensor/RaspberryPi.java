package marchandivan.RoomMonitoring.sensor;


import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import marchandivan.RoomMonitoring.R;
import marchandivan.RoomMonitoring.db.DeviceConfig;
import marchandivan.RoomMonitoring.db.SensorConfig;
import marchandivan.RoomMonitoring.http.RestClient;

import static marchandivan.RoomMonitoring.db.SensorConfig.TypeFromString;

/**
 * Created by ivan on 9/5/16.
 */
public class RaspberryPi extends Sensor {
    public RaspberryPi() {
        super();
    }

    @Override
    public int getIcon() {
        return R.drawable.raspberry;
    }

    @Override
    public String getDisplayName() {
        return "Raspberry Pi";
    }

    public AuthType getAuthType() {
        return AuthType.USER_PASSWORD;
    }

    public AuthEncoding getAuthEncoding() {
        return AuthEncoding.HEADER;
    }

    public boolean needHostUrl() {
        return true;
    }

    @Override
    public ConnectionResult testConnection(Context context, DeviceConfig deviceConfig) {
        try {
            RestClient restClient = this.getRestClient(context, deviceConfig);
            restClient.setShowSslConfirmDialog(true);
            JSONObject result = restClient.getJson("/api/v1/login");
            if (restClient.getHttpStatusCode() != HttpURLConnection.HTTP_OK) {
                return ConnectionResult.UNABLE_TO_CONNECT;
            } else if (result != null && result.has("success") && result.getBoolean("success")) {
                return ConnectionResult.SUCCESS;
            } else {
                return ConnectionResult.AUTH_FAILED;
            }
        } catch (Exception e) {
            return ConnectionResult.FAILURE;
        }
    }

    public void updateSensor(Context context, DeviceConfig deviceConfig, List<SensorConfig> sensorConfigs) {
        // Get Device
        RestClient restClient = getRestClient(context, deviceConfig);
        JSONArray rooms = restClient.getJsonArray("/api/v1/get/rooms");
        for (int i = 0 ; i < rooms.length() ; i++) {
            try {
                JSONObject reply = rooms.getJSONObject(i);
                // Update the corresponding sensor config
                for (SensorConfig sensorConfig: sensorConfigs) {
                    if (sensorConfig.getConfigString("room").equals(reply.getString("room"))) {
                        Log.d("RaspberryPi", "Update sensor " + sensorConfig.getId());
                        JSONObject data = new JSONObject();
                        data.put("temperature", reply.get("temperature"));
                        data.put("humidity", reply.get("humidity"));
                        if (sensorConfig.getType() == SensorConfig.Type.THERMOSTAT) {
                            JSONObject thermostat = new JSONObject();
                            if (reply.has("temperatureCommand")) {
                                thermostat.put("temperature", reply.get("temperatureCommand"));
                            }
                            if (reply.has("mode")) {
                                thermostat.put("mode", reply.get("mode"));
                            }
                            if (thermostat.length() != 0){
                                data.put("thermostat", thermostat);
                            }
                        }
                        sensorConfig.update(data);
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean addSensors(Context context, final DeviceConfig deviceConfig) {
        boolean result = false;
        RestClient restClient = getRestClient(context, deviceConfig);
        JSONObject getRoomListResult = restClient.getJson("/api/v1/get/roomList");
        if (getRoomListResult.has("room_list")) {
            try {
                JSONArray roomList = getRoomListResult.getJSONArray("room_list");
                for (int i = 0 ; i < roomList.length() ; i++ ) {
                    JSONObject config = new JSONObject();
                    JSONObject room = roomList.getJSONObject(i);
                    String roomName = room.getString("room");
                    config.put("room", roomName);
                    SensorConfig.Type sensorType = TypeFromString(room.getString("type"));
                    SensorConfig sensorConfig = new SensorConfig(context, roomName, sensorType, deviceConfig.getId(), config);
                    sensorConfig.add();
                    result = true;
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        return result;
    }

    @Override
    protected void setThermostatImpl(Context context, DeviceConfig deviceConfig, SensorConfig sensorConfig, SensorConfig.ThermostatMode mode, Integer temperature) {
        try {
            RestClient restClient = getRestClient(context, deviceConfig);
            String path = "/api/v1/set/thermostat/" + sensorConfig.getConfigString("room").replace(" ", "%20") + "/" + SensorConfig.ModeToString(mode);
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

    public ArrayList<ConfigField> getConfigFields() {
        ArrayList<ConfigField> fields = new ArrayList<>();
        fields.add(new ConfigField("room", "Room Name", ConfigFieldType.TEXT_LIST, true));
        return fields;
    }

}
