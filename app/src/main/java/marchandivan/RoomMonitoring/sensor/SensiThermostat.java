package marchandivan.RoomMonitoring.sensor;


import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import marchandivan.RoomMonitoring.db.DeviceConfig;
import marchandivan.RoomMonitoring.db.SensorConfig;
import marchandivan.RoomMonitoring.http.RestClient;

import static marchandivan.RoomMonitoring.db.SensorConfig.ModeToString;

/**
 * Created by ivan on 9/5/16.
 */
public class SensiThermostat extends Sensor {
    private HashMap<Long, String> mConnectionTokens = new HashMap<>();
    private HashMap<Long, String> mGroupsTokens = new HashMap<>();
    private HashMap<Long, String> mMessageId = new HashMap<>();
    private HashMap<Long, HashMap<String, JSONObject>> mMeasureCache = new HashMap<>();
    private HashMap<Long, Integer> mSessionIterator = new HashMap<>();

    public SensiThermostat() {
        super();
    }

    /*@Override
    /public int getIcon() {
        return R.drawable.sensi_logo;
    }*/

    @Override
    public String getDisplayName() {
        return "Sensi Thermostat";
    }

    public AuthType getAuthType() {
        return AuthType.USER_PASSWORD;
    }

    public AuthEncoding getAuthEncoding() {
        return AuthEncoding.NONE;
    }

    public boolean needHostUrl() {
        return false;
    }

    private void clearCache(DeviceConfig deviceConfig) {
        mConnectionTokens.remove(deviceConfig.getId());
        mGroupsTokens.remove(deviceConfig.getId());
        mMessageId.remove(deviceConfig.getId());
        mMeasureCache.remove(deviceConfig.getId());
        mSessionIterator.remove(deviceConfig.getId());
    }

    private Integer getNextSessionIterator(DeviceConfig deviceConfig) {
        Integer iterator = 0;
        if (mSessionIterator.containsKey(deviceConfig.getId())) {
            iterator = mSessionIterator.get(deviceConfig.getId());
            iterator++;
        }
        mSessionIterator.put(deviceConfig.getId(), iterator);
        return iterator;
    }

    private RestClient getRestClient(Context context, DeviceConfig deviceConfig, boolean needConnectionToken, boolean needMessageId) {
        RestClient restClient = new RestClient(context, true, "bus-serv.sensicomfort.com", 443);

        // ConnectionToken
        if (needConnectionToken && mConnectionTokens.containsKey(deviceConfig.getId())) {
            restClient.addUrlParam("transport", "longPolling");
            restClient.addUrlParam("connectionToken", mConnectionTokens.get(deviceConfig.getId()));
            // GroupsToken
            if (mGroupsTokens.containsKey(deviceConfig.getId())) {
                restClient.addUrlParam("groupsToken", mGroupsTokens.get(deviceConfig.getId()));
            }
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
        restClient.setHttpRequestCookies(GetCookieManager(deviceConfig).getCookieStore().getCookies());

        return restClient;
    }

    @Override
    public ConnectionResult testConnection(Context context, DeviceConfig deviceConfig) {
        return login(context, deviceConfig);
    }

    private ConnectionResult login(Context context, DeviceConfig deviceConfig) {
        try {
            // Clear cache from previous connection
            clearCache(deviceConfig);
            // Get new RestClient
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
            e.printStackTrace();
        }
    }


    private void initConnection(Context context, DeviceConfig deviceConfig) {
        // Login if necessary
        if (GetCookieManager(deviceConfig).getCookieStore().getCookies().isEmpty()) {
            login(context, deviceConfig);
        }

        // Negociate if necessary
        if (!mConnectionTokens.containsKey(deviceConfig.getId())) {
            negotiate(context, deviceConfig);
            connect(context, deviceConfig);
        }
    }

    private void subscribe(Context context, DeviceConfig deviceConfig, List<SensorConfig> sensorConfigs) {
        // Get Rest client
        RestClient restClient = getRestClient(context, deviceConfig, true, false);

        // {"H":"thermostat-v1","M":"Subscribe","A":["36-6f-92-ff-fe-03-fd-34"],"I":0}
        JSONObject postData = new JSONObject();
        try {
            postData.put("H", "thermostat-v1");
            postData.put("M", "Subscribe");
            postData.put("I", getNextSessionIterator(deviceConfig));
            JSONArray icds = new JSONArray();
            for (SensorConfig sensorConfig: sensorConfigs) {
                icds.put(sensorConfig.getConfigString("ICD"));
            }
            postData.put("A", icds);
        } catch (Exception e) {
            // Ignore
        }

        restClient.post("/realtime/send", "application/x-www-form-urlencoded; charset=UTF-8", "data=" + Uri.encode(postData.toString()));
    }

    protected void setThermostatImpl(Context context, DeviceConfig deviceConfig, SensorConfig sensorConfig, SensorConfig.ThermostatMode mode, Integer temperature) {
        try {
            // Get Rest client
            RestClient restClient = getRestClient(context, deviceConfig, true, false);

            // Get/Create caches
            if (!mMeasureCache.containsKey(deviceConfig.getId())) {
                mMeasureCache.put(deviceConfig.getId(), new HashMap<String, JSONObject>());
            }
            HashMap<String, JSONObject> jsonCache = mMeasureCache.get(deviceConfig.getId());
            String icd = sensorConfig.getConfigString("ICD");
            if (!jsonCache.containsKey(icd)) {
                jsonCache.put(icd, new JSONObject());
            }
            JSONObject thermostatCache = jsonCache.get(icd);
            if (!thermostatCache.has("EnvironmentControls")) {
                thermostatCache.put("EnvironmentControls", new JSONObject());
            }
            JSONObject environmentControls = jsonCache.get(icd).getJSONObject("EnvironmentControls");

            // Build data to be posted and update cache
            JSONObject postData = new JSONObject();
            postData.put("H", "thermostat-v1");
            if (temperature != null) {
                int C = (int) F2C(temperature);
                switch (mode) {
                    case HEAT:
                        JSONObject heatSetPoint = new JSONObject();
                        heatSetPoint.put("F", temperature);
                        heatSetPoint.put("C", C);
                        environmentControls.put("HeatSetpoint", heatSetPoint);
                        postData.put("M", "SetHeat");
                        break;
                    case COOL:
                        JSONObject coolSetPoint = environmentControls.getJSONObject("CoolSetpoint");
                        coolSetPoint.put("F", temperature);
                        coolSetPoint.put("C", C);
                        environmentControls.put("CoolSetpoint", coolSetPoint);
                        postData.put("M", "SetCool");
                        break;
                }
                postData.put("I", getNextSessionIterator(deviceConfig));
                JSONArray param = new JSONArray();
                param.put(icd);
                param.put(temperature);
                param.put("F");
                postData.put("A", param);
            } else {
                postData.put("M", "SetSystemMode");
                postData.put("I", getNextSessionIterator(deviceConfig));
                JSONArray param = new JSONArray();
                param.put(sensorConfig.getConfigString("ICD"));
                switch (mode) {
                    case HEAT:
                        environmentControls.put("SystemMode", "Heat");
                        param.put("Heat");
                        break;
                    case COOL:
                        environmentControls.put("SystemMode", "Cool");
                        param.put("Cool");
                        break;
                    case OFF:
                    default:
                        environmentControls.put("SystemMode", "Off");
                        param.put("Off");
                        break;
                }
                postData.put("A", param);
            }
            Log.d("SensiThermostat", "POST Json : " + postData.toString());
            restClient.post("/realtime/send", "application/x-www-form-urlencoded; charset=UTF-8", "data=" + Uri.encode(postData.toString()));
            if (restClient.getHttpStatusCode() != HttpURLConnection.HTTP_OK) {
                // Reset Connection in case of failure
                clearCache(deviceConfig);
                initConnection(context, deviceConfig);

                // Try again
                restClient.post("/realtime/send", "application/x-www-form-urlencoded; charset=UTF-8", "data=" + Uri.encode(postData.toString()));
            }

            // Update cache
            thermostatCache.put("EnvironmentControls", environmentControls);
            jsonCache.put(icd, thermostatCache);
            mMeasureCache.put(deviceConfig.getId(), jsonCache);

        } catch (Exception e) {
            // Ignore
            e.printStackTrace();
        }
    }

    private void updateCache(DeviceConfig deviceConfig, JSONObject result) {
        try {
            if (!mMeasureCache.containsKey(deviceConfig.getId())) {
                mMeasureCache.put(deviceConfig.getId(), new HashMap<String, JSONObject>());
            }
            HashMap<String, JSONObject> jsonCache = mMeasureCache.get(deviceConfig.getId());
            if (result.has("M")) {
                JSONArray M = result.getJSONArray("M");
                // Any measure?
                for (int i = 0; i < M.length(); i++) {
                    JSONObject thermostatConfig = M.getJSONObject(i);
                    JSONObject thermostatStatus = thermostatConfig.getJSONArray("A").getJSONObject(1);
                    String icd = thermostatConfig.getJSONArray("A").getString(0);
                    // Update Cache
                    if (thermostatConfig.getString("M").equals("online") || !jsonCache.containsKey(icd)) {
                        jsonCache.put(icd, thermostatStatus);
                    } else {
                        // Update cache
                        Iterator<String> keys = thermostatStatus.keys();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            if (!jsonCache.get(icd).has(key)) {
                                jsonCache.get(icd).put(key, thermostatStatus.get(key));
                            } else {
                                Iterator<String> subKeys = thermostatStatus.getJSONObject(key).keys();
                                while (subKeys.hasNext()) {
                                    String subKey = subKeys.next();
                                    jsonCache.get(icd).getJSONObject(key).put(subKey, thermostatStatus.getJSONObject(key).get(subKey));
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void updateSensorConfig(DeviceConfig deviceConfig, SensorConfig sensorConfig) {
        try {
            if (mMeasureCache.containsKey(deviceConfig.getId())) {
                HashMap<String, JSONObject> jsonCache = mMeasureCache.get(deviceConfig.getId());
                String icd = sensorConfig.getConfigString("ICD");
                if (icd != null && jsonCache.containsKey(icd)) {
                    // Get temp and humidity
                    JSONObject data = new JSONObject();
                    Log.d("SensiThermostat", "Cache : " + jsonCache.get(icd).toString());
                    if (jsonCache.get(icd).has("OperationalStatus")) {
                        JSONObject operationalStatus = jsonCache.get(icd).getJSONObject("OperationalStatus");
                        double temperature = operationalStatus.getJSONObject("Temperature").getInt("F");
                        double humidity = operationalStatus.getInt("Humidity");
                        data.put("temperature", temperature);
                        data.put("humidity", humidity);
                    }

                    // Heater/AC config
                    if (jsonCache.get(icd).has("EnvironmentControls")) {
                        JSONObject thermostat = new JSONObject();
                        JSONObject environmentControls = jsonCache.get(icd).getJSONObject("EnvironmentControls");
                        if (environmentControls.has("SystemMode")) {
                            switch (environmentControls.getString("SystemMode")) {
                                case "Heat":
                                    JSONObject heatSetPoint = environmentControls.getJSONObject("HeatSetpoint");
                                    thermostat.put("temperature", heatSetPoint.get("F"));
                                    thermostat.put("mode", "HEAT");
                                    break;
                                case "Cool":
                                    JSONObject coolSetPoint = environmentControls.getJSONObject("CoolSetpoint");
                                    thermostat.put("temperature", coolSetPoint.get("F"));
                                    thermostat.put("mode", "COOL");
                                    break;
                                case "Off":
                                default:
                                    thermostat.put("mode", "OFF");
                                    break;
                            }
                        }
                        if (thermostat.length() != 0) {
                            data.put("thermostat", thermostat);
                        }
                    }

                    Log.d("SensiThermostat", "Update sensor " + sensorConfig.getId() + " " + sensorConfig.getName());
                    sensorConfig.update(data);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateSensor(Context context, DeviceConfig deviceConfig, List<SensorConfig> sensorConfigs) {
        // Init connection
        initConnection(context, deviceConfig);
        if (!mGroupsTokens.containsKey(deviceConfig.getId())) {
            subscribe(context, deviceConfig, sensorConfigs);
        }

        // Get Rest client
        RestClient restClient = getRestClient(context, deviceConfig, true, true);
        JSONObject result = restClient.getJson("/realtime/poll");
        if (restClient.getHttpStatusCode() != HttpURLConnection.HTTP_OK || result.length() == 0) {
            // Reset Connection in case of failure
            clearCache(deviceConfig);
            initConnection(context, deviceConfig);

            // Try again
            result = restClient.getJson("/realtime/poll");
        }

        try {
            // Update message Id
            if (result.has("C") && result.get("C") instanceof String) {
                mMessageId.put(deviceConfig.getId(), result.getString("C"));
            }
            // Update groups token
            if (result.has("G") && result.get("G") instanceof String) {
                mGroupsTokens.put(deviceConfig.getId(), result.getString("G"));
            }

            // Update thermostat status
            updateCache(deviceConfig, result);

            // Update sensor config
            for (SensorConfig sensorConfig : sensorConfigs) {
                updateSensorConfig(deviceConfig, sensorConfig);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean addSensors(Context context, final DeviceConfig deviceConfig) {
        boolean result = false;
        // Init connection
        initConnection(context, deviceConfig);

        RestClient restClient = getRestClient(context, deviceConfig, false, false);
        JSONArray thermostats = restClient.getJsonArray("/api/thermostats");
        for (int i = 0 ; i < thermostats.length() ; i++) {
            try {
                JSONObject thermostat = thermostats.getJSONObject(i);
                if (thermostat.has("DeviceName") && thermostat.get("DeviceName") instanceof String) {
                    SensorConfig sensorConfig = new SensorConfig(context, thermostat.getString("DeviceName"), SensorConfig.Type.THERMOSTAT, deviceConfig.getId(), thermostat);
                    sensorConfig.add();
                    result = true;
                }
            } catch (Exception e) {
                // Ignore
                e.printStackTrace();
            }
        }
        return result;
    }

    public ArrayList<ConfigField> getConfigFields() {
        ArrayList<ConfigField> fields = new ArrayList<>();
        fields.add(new ConfigField("thermostat", "Thermostat", ConfigFieldType.TEXT_LIST, true));
        return fields;
    }

    // Converts to celcius
    private double F2C(double fahrenheit) {
        return ((fahrenheit - 32) * 5 / 9);
    }

    // Converts to fahrenheit
    private double C2F(double celsius) {
        return ((celsius * 9) / 5) + 32;
    }
}
