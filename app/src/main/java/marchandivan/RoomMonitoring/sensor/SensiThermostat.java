package marchandivan.RoomMonitoring.sensor;


import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import marchandivan.RoomMonitoring.R;
import marchandivan.RoomMonitoring.db.DeviceConfig;
import marchandivan.RoomMonitoring.db.SensorConfig;
import marchandivan.RoomMonitoring.http.RestClient;

/**
 * Created by ivan on 9/5/16.
 */
public class SensiThermostat extends Sensor {
    private HashMap<Long, String> mConnectionTokens = new HashMap<>();
    private HashMap<Long, String> mMessageId = new HashMap<>();
    private HashMap<String, JSONArray> mMeasureCache = new HashMap<>();

    public SensiThermostat() {
        super(Type.THERMOSTAT);
    }

    @Override
    public int getIcon() {
        return R.drawable.sensi_logo;
    }

    @Override
    public String getDisplayName() {
        return "Sensi Thermostat";
    }

    public AuthType getAuthType() {
        return AuthType.USER_PASSWORD;
    }

    public boolean needHostUrl() {
        return false;
    }

    public RestClient getRestClient(Context context, DeviceConfig deviceConfig, boolean needConnectionToken, boolean needMessageId) {
        RestClient restClient = new RestClient(context, true, "bus-serv.sensicomfort.com", 443);

        // ConnectionToken
        if (needConnectionToken && mConnectionTokens.containsKey(deviceConfig.getId())) {
            restClient.addUrlParam("transport", "longPolling");
            restClient.addUrlParam("connectionToken", mConnectionTokens.get(deviceConfig.getId()));
        }

        // Message Id
        if (needMessageId && mMessageId.containsKey(deviceConfig.getId())) {
            restClient.addUrlParam("messageId", mMessageId.get(deviceConfig.getId()));
            restClient.addUrlParam("connectionData", "[{\"name\":\"thermostat-v1\"}]");
        }

        // Setup header
        restClient.setRequestProperty("Accept", "application/json; version=1, */*; q=0.01");
        restClient.setRequestProperty("X-Requested-With", "XMLHttpRequest");

        // Setup cookies
        for (HttpCookie cookie: GetCookieManager(deviceConfig).getCookieStore().getCookies()) {
            Log.d("SensiThermostat", cookie.toString());
        }
        restClient.setHttpRequestCookies(GetCookieManager(deviceConfig).getCookieStore().getCookies());

        return restClient;
    }

    @Override
    public ConnectionResult testConnection(Context context, DeviceConfig deviceConfig) {
        return login(context, deviceConfig);
    }

    private ConnectionResult login(Context context, DeviceConfig deviceConfig) {
        try {
            RestClient restClient = getRestClient(context, deviceConfig, false, false);
            // Build the json to post : {"UserName": "user", "Password": "password"}
            JSONObject postData = new JSONObject();
            postData.put("UserName", deviceConfig.getUser());
            postData.put("Password", deviceConfig.getPassword());

            // Send the post request
            JSONObject result = restClient.postJson("/api/authorize", postData);
            if (restClient.getHttpStatusCode() != HttpURLConnection.HTTP_OK) {
                return ConnectionResult.UNABLE_TO_CONNECT;
            } else if (result != null) {
                // Store cookies
                if (!restClient.getHttpResponseCookies().isEmpty()) {
                    for (HttpCookie cookie: restClient.getHttpResponseCookies()) {
                        GetCookieManager(deviceConfig).getCookieStore().add(null, cookie);
                    }
                }
                return ConnectionResult.SUCCESS;
            } else {
                return ConnectionResult.AUTH_FAILED;
            }
        } catch (Exception e) {
            return ConnectionResult.FAILURE;
        }
    }

    private void negotiate(Context context, DeviceConfig deviceConfig) {
        // Get Rest client
        RestClient restClient = getRestClient(context, deviceConfig, false, false);
        JSONObject result = restClient.getJson("/realtime/negotiate");
        try {
            if (result.has("ConnectionToken") && result.get("ConnectionToken") instanceof String) {
                mConnectionTokens.put(deviceConfig.getId(), result.getString("ConnectionToken"));
            }
        } catch (Exception e) {
            // Ignore
        }
    }


    private void connect(Context context, DeviceConfig deviceConfig) {
        // Get Rest client
        RestClient restClient = getRestClient(context, deviceConfig, true, false);
        JSONObject result = restClient.getJson("/realtime/connect");
        try {
            if (result.has("C") && result.get("C") instanceof String) {
                mMessageId.put(deviceConfig.getId(), result.getString("C"));
            }
        } catch (Exception e) {
            // Ignore
        }
    }


    private void initConnection(Context context, DeviceConfig deviceConfig) {
        // Login if necessary
        if (GetCookieManager(deviceConfig).getCookieStore().getCookies().isEmpty()) {
            login(context, deviceConfig);
            // Force negociation
            mConnectionTokens.remove(deviceConfig.getId());
        }

        // Negociate if necessary
        if (!mConnectionTokens.containsKey(deviceConfig.getId())) {
            negotiate(context, deviceConfig);
            connect(context, deviceConfig);
            subscribe(context, deviceConfig);
        }
    }

    private void subscribe(Context context, DeviceConfig deviceConfig) {
        // Get Rest client
        RestClient restClient = getRestClient(context, deviceConfig, true, false);

        // {"H":"thermostat-v1","M":"Subscribe","A":["36-6f-92-ff-fe-03-fd-34"],"I":0}
        JSONObject postData = new JSONObject();
        try {
            postData.put("H", "thermostat-v1");
            postData.put("M", "Subscribe");
            postData.put("I", 0);
            JSONArray icds = new JSONArray();
            icds.put("36-6f-92-ff-fe-03-fd-34");
            postData.put("A", icds);
        } catch (Exception e) {
            // Ignore
        }

        restClient.post("/realtime/send", "application/x-www-form-urlencoded; charset=UTF-8", "data=" + Uri.encode(postData.toString()));
    }

    public JSONObject getSensorMeasure(Context context, SensorConfig sensorConfig) {
        // Get associated device config
        DeviceConfig deviceConfig = new DeviceConfig(context, sensorConfig.getDeviceId());
        deviceConfig.read();

        // Init connection
        initConnection(context, deviceConfig);

        // Get Rest client
        RestClient restClient = getRestClient(context, deviceConfig, true, true);
        JSONObject result = restClient.getJson("/realtime/poll");

        // Get temp and humidity
        JSONObject measure = new JSONObject();
        try {
            JSONArray M = result.getJSONArray("M");
            if (M.length() == 0) {
                // Use cache as there is no update from the server
                M = mMeasureCache.get("36-6f-92-ff-fe-03-fd-34");
            } else {
                // Update cache
                mMeasureCache.put("36-6f-92-ff-fe-03-fd-34", M);
            }
            // Any measure?
            if (M.length() > 0) {
                JSONObject operationalStatus = M.getJSONObject(0).getJSONArray("A").getJSONObject(1).getJSONObject("OperationalStatus");
                double temperature = operationalStatus.getJSONObject("Temperature").getInt("F");
                double humidity = operationalStatus.getInt("Humidity");
                measure.put("temperature", temperature);
                measure.put("humidity", humidity);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return measure;
    }

    public ArrayList<ConfigField> getConfigFields() {
        ArrayList<ConfigField> fields = new ArrayList<>();
        fields.add(new ConfigField("thermostat", "Thermostat", ConfigFieldType.TEXT_LIST, true));
        return fields;
    }

    public ArrayList<String> getTextFieldValuesImpl(Context context, final DeviceConfig deviceConfig, final String key) {

        // Init connection
        initConnection(context, deviceConfig);

        ArrayList<String> result = null;
        RestClient restClient = getRestClient(context, deviceConfig, false, false);
        switch (key) {
            case "thermostat":
                result = new ArrayList<>();
                JSONArray thermostats = restClient.getJsonArray("/api/thermostats");
                for (int i = 0 ; i < thermostats.length() ; i++) {
                    try {
                        JSONObject thermostat = thermostats.getJSONObject(i);
                        if (thermostat.has("DeviceName") && thermostat.get("DeviceName") instanceof String) {
                            result.add(thermostat.getString("DeviceName"));
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                }
                break;
            default:
                break;
        }
        return result;
    }

}
