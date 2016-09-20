package marchandivan.RoomMonitoring.sensor;


import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.ArrayList;

import marchandivan.RoomMonitoring.R;
import marchandivan.RoomMonitoring.db.DeviceConfig;
import marchandivan.RoomMonitoring.db.SensorConfig;
import marchandivan.RoomMonitoring.http.RestClient;

/**
 * Created by ivan on 9/5/16.
 */
public class RaspberryPi extends Sensor {
    public RaspberryPi() {
        super(Type.THERMOMETER);
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

    public boolean needHostUrl() {
        return true;
    }

    @Override
    public ConnectionResult testConnection(Context context, DeviceConfig deviceConfig) {
        try {
            RestClient restClient = this.getRestClient(context, deviceConfig);
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

    public JSONObject getSensorMeasure(Context context, SensorConfig sensorConfig) {
        // Get Device
        RestClient restClient = getRestClient(context, sensorConfig);
        return restClient.getJson("/api/v1/get/temperature/" + sensorConfig.getConfigString("room"));
    }

    public ArrayList<ConfigField> getConfigFields() {
        ArrayList<ConfigField> fields = new ArrayList<>();
        fields.add(new ConfigField("room", "Room Name", ConfigFieldType.TEXT_LIST, true));
        return fields;
    }

    public ArrayList<String> getTextFieldValuesImpl(Context context, final DeviceConfig deviceConfig, final String key) {
        ArrayList<String> result = null;
        RestClient restClient = getRestClient(context, deviceConfig);
        switch (key) {
            case "room":
                result = new ArrayList<>();
                JSONObject getRoomListResult = restClient.getJson("/api/v1/get/roomList");
                if (getRoomListResult.has("room_list")) {
                    try {
                        JSONArray roomList = getRoomListResult.getJSONArray("room_list");
                        for (int i = 0 ; i < roomList.length() ; i++ ) {
                            result.add(roomList.getString(i));
                        }
                    } catch (Exception e){
                    }
                }
                break;
            default:
                break;
        }
        return result;
    }

}
