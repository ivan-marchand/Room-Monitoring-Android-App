package marchandivan.RoomMonitoring.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedList;

import marchandivan.RoomMonitoring.sensor.Sensor;


/**
 * Created by ivan on 10/26/15.
 */
public class SensorConfig {

    // Expiration time after which the temperature measure is not valid anymore
    static final long mpMeasureExpirationTime = 10 * 60 * 1000; // 10 minutes

    private long mId = 0;
    private ConfigDbHelper mDbHelper;
    private String mSensorName;
    private Type mType;
    private boolean mVisible = true;
    private long mLastUpdate = 0;
    private long mLastAlarm = 0;
    private JSONObject mData = new JSONObject();
    private long mDeviceId = 0;
    private JSONObject mConfig = new JSONObject();

    public final long getId() {
        return mId;
    }

    public final String getName() {
        return mSensorName;
    }

    public enum Type {
        THERMOMETER, THERMOSTAT
    }

    public static String TypeToString(Type type) {
        switch (type) {
            case THERMOMETER:
                return "thermometer";
            case THERMOSTAT:
                return "thermostat";
        }
        return "";
    }

    public static Type TypeFromString(final String string) {
        switch (string) {
            case "thermostat":
                return Type.THERMOSTAT;
            case "thermometer":
                return Type.THERMOMETER;
        }
        return Type.THERMOMETER;
    }

    public Type getType() {
        return mType;
    }

    public final long getLastAlarm() {
        return mLastAlarm;
    }

    public final JSONObject getData() {
        return mData;
    }

    public void setData(final JSONObject data) {
        mLastUpdate = System.currentTimeMillis();
        mData = data;
    }

    public long getDeviceId() {
        return mDeviceId;
    }

    public boolean isVisible() {
        return mVisible;
    }

    public String getConfigString(final String field) {
        try {
            return mConfig.getString(field);
        } catch (Exception e) {
            return "";
        }
    }

    public SensorConfig(Context context, final String sensorName, final Type type, long deviceId, JSONObject config) {
        mDbHelper = new ConfigDbHelper(context);
        mSensorName = sensorName;
        mType = type;
        mDeviceId = deviceId;
        mConfig = config;
        mVisible = true;
    }

    static public HashMap<Long, SensorConfig> GetMap(Context context) {
        HashMap<Long, SensorConfig> sensorConfigs = new HashMap<Long, SensorConfig>();
        ConfigDbHelper dbHelper = new ConfigDbHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] projection = {
                SensorConfigContract.SensorEntry._ID,
        };

        Cursor cursor = db.query(
                SensorConfigContract.SensorEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                null,                                     // The columns for the WHERE clause
                null,                                     // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                      // The sort order
        );

        // Anything found
        if (cursor.getCount() == 0) {
            db.close();
        }
        // Get values
        cursor.moveToFirst();
        for (int i = 0 ; i < cursor.getCount() ; i++) {
            Long id = cursor.getLong(cursor.getColumnIndexOrThrow(SensorConfigContract.SensorEntry._ID));
            Log.d("SensorConfig:readList", "Sensor " + id);
            SensorConfig sensorConfig = new SensorConfig(context, id);
            if (sensorConfig.read()) {
                sensorConfigs.put(id, sensorConfig);
            }
            cursor.moveToNext();
        }

        db.close();
        return sensorConfigs;

    }

    static public LinkedList<SensorConfig> GetList(Context context, long deviceId) {
        LinkedList<SensorConfig> sensorConfigs = new LinkedList<>();
        ConfigDbHelper dbHelper = new ConfigDbHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] projection = {
                SensorConfigContract.SensorEntry._ID,
        };

        String[] selectionArgs = {String.valueOf(deviceId)};
        Cursor cursor = db.query(
                SensorConfigContract.SensorEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                SensorConfigContract.SensorEntry.COLUMN_NAME_DEVICE + "=?",   // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                      // The sort order
        );

        // Anything found
        if (cursor.getCount() == 0) {
            db.close();
        }
        // Get values
        cursor.moveToFirst();
        for (int i = 0 ; i < cursor.getCount() ; i++) {
            Long id = cursor.getLong(cursor.getColumnIndexOrThrow(SensorConfigContract.SensorEntry._ID));
            Log.d("SensorConfig:readList", "Sensor " + id);
            SensorConfig sensorConfig = new SensorConfig(context, id);
            if (sensorConfig.read()) {
                sensorConfigs.add(sensorConfig);
            }
            cursor.moveToNext();
        }

        db.close();
        return sensorConfigs;

    }

    public static boolean MeasureHasExpired(Context context) {
        // Check if all of the room temp/humidity measure have expired
        for (SensorConfig sensorConfig : GetMap(context).values()) {
            if (sensorConfig.isVisible() && sensorConfig.measureHasExpired()) {
                return true;
            }
        }
        return false;
    }

    public SensorConfig(Context context, long id) {
        mId = id;
        mDbHelper = new ConfigDbHelper(context);
    }

    public SensorConfig(Context context, String sensorName, long lastUpdate, JSONObject data) {
        mSensorName = sensorName;
        mDbHelper = new ConfigDbHelper(context);
        mLastUpdate = lastUpdate;
        mData = data;
    }

    public void add() {

        // Gets the data repository in wyrite mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(SensorConfigContract.SensorEntry.COLUMN_NAME_SENSOR, mSensorName);
        values.put(SensorConfigContract.SensorEntry.COLUMN_NAME_TYPE, TypeToString(mType));
        values.put(SensorConfigContract.SensorEntry.COLUMN_NAME_VISIBLE, mVisible ? 1 : 0);
        values.put(SensorConfigContract.SensorEntry.COLUMN_NAME_LAST_UPDATE, mLastUpdate);
        values.put(SensorConfigContract.SensorEntry.COLUMN_NAME_LAST_ALARM, mLastAlarm);
        values.put(SensorConfigContract.SensorEntry.COLUMN_NAME_DATA, mData.toString());
        values.put(SensorConfigContract.SensorEntry.COLUMN_NAME_DEVICE, mDeviceId);
        values.put(SensorConfigContract.SensorEntry.COLUMN_NAME_CONFIG, mConfig.toString());

        // Insert the new row, returning the primary key value of the new row
        mId = db.insert(
                SensorConfigContract.SensorEntry.TABLE_NAME,
                null,
                values);
        db.close();
    }

    public void delete() {
        // Gets the data repository in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        // Define 'where' part of query.
        String selection = SensorConfigContract.SensorEntry._ID + " =?";
        // Specify arguments in placeholder order.
        String[] selectionArgs = {String.valueOf(mId)};
        // Issue SQL statement.
        db.delete(SensorConfigContract.SensorEntry.TABLE_NAME, selection, selectionArgs);
        db.close();
    }

    public boolean read() {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        String[] projection = {
                SensorConfigContract.SensorEntry.COLUMN_NAME_SENSOR,
                SensorConfigContract.SensorEntry.COLUMN_NAME_TYPE,
                SensorConfigContract.SensorEntry.COLUMN_NAME_VISIBLE,
                SensorConfigContract.SensorEntry.COLUMN_NAME_LAST_UPDATE,
                SensorConfigContract.SensorEntry.COLUMN_NAME_LAST_ALARM,
                SensorConfigContract.SensorEntry.COLUMN_NAME_DATA,
                SensorConfigContract.SensorEntry.COLUMN_NAME_DEVICE,
                SensorConfigContract.SensorEntry.COLUMN_NAME_CONFIG
        };

        String[] selectionArgs = {String.valueOf(mId)};
        Cursor cursor = db.query(
                SensorConfigContract.SensorEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                SensorConfigContract.SensorEntry._ID + "=?",         // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                      // The sort order
        );

        // Anything found
        if (cursor.getCount() == 0) {
            db.close();
            return false;
        }

        // Get values
        cursor.moveToFirst();
        mSensorName = cursor.getString(cursor.getColumnIndexOrThrow(SensorConfigContract.SensorEntry.COLUMN_NAME_SENSOR));
        mType = TypeFromString(cursor.getString(cursor.getColumnIndexOrThrow(SensorConfigContract.SensorEntry.COLUMN_NAME_TYPE)));
        mVisible = cursor.getInt(cursor.getColumnIndexOrThrow(SensorConfigContract.SensorEntry.COLUMN_NAME_VISIBLE)) > 0;
        mLastUpdate = cursor.getLong(cursor.getColumnIndexOrThrow(SensorConfigContract.SensorEntry.COLUMN_NAME_LAST_UPDATE));
        mLastAlarm = cursor.getLong(cursor.getColumnIndexOrThrow(SensorConfigContract.SensorEntry.COLUMN_NAME_LAST_ALARM));
        mDeviceId = cursor.getLong(cursor.getColumnIndexOrThrow(SensorConfigContract.SensorEntry.COLUMN_NAME_DEVICE));
        try {
            mData = new JSONObject(cursor.getString(cursor.getColumnIndexOrThrow(SensorConfigContract.SensorEntry.COLUMN_NAME_DATA)));
        } catch (JSONException e) {
            // Unable to decode
            mData = new JSONObject();
        }
        try {
            mConfig = new JSONObject(cursor.getString(cursor.getColumnIndexOrThrow(SensorConfigContract.SensorEntry.COLUMN_NAME_CONFIG)));
        } catch (JSONException e) {
            // Unable to decode
            mConfig = new JSONObject();
        }

        db.close();
        return true;
    }

    public void update(JSONObject data) {
        mLastUpdate = System.currentTimeMillis();
        mData = data;

        // Gets the data repository in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // New value for one column
        ContentValues values = new ContentValues();
        values.put(SensorConfigContract.SensorEntry.COLUMN_NAME_LAST_UPDATE, mLastUpdate);
        values.put(SensorConfigContract.SensorEntry.COLUMN_NAME_DATA, mData.toString());

        // Which row to update, based on the ID
        String selection = SensorConfigContract.SensorEntry._ID + "=?";
        String[] selectionArgs = {String.valueOf(mId)};

        int count = db.update(
                SensorConfigContract.SensorEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs);
    }

    public void updateName(String name) {
        mLastUpdate = System.currentTimeMillis();
        mSensorName = name;

        // Gets the data repository in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // New value for one column
        ContentValues values = new ContentValues();
        values.put(SensorConfigContract.SensorEntry.COLUMN_NAME_SENSOR, mSensorName);

        // Which row to update, based on the ID
        String selection = SensorConfigContract.SensorEntry._ID + "=?";
        String[] selectionArgs = {String.valueOf(mId)};

        int count = db.update(
                SensorConfigContract.SensorEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs);
    }

    public void updateAlarm() {
        mLastAlarm = System.currentTimeMillis();
        // Gets the data repository in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // New value for one column
        ContentValues values = new ContentValues();
        values.put(SensorConfigContract.SensorEntry.COLUMN_NAME_LAST_ALARM, mLastAlarm);

        // Which row to update, based on the ID
        String selection = SensorConfigContract.SensorEntry._ID + "=?";
        String[] selectionArgs = {String.valueOf(mId)};

        int count = db.update(
                SensorConfigContract.SensorEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs);
        db.close();
    }

    public void updateVisibility(boolean visible) {
        if (mVisible != visible) {
            mVisible = visible;
            // Gets the data repository in write mode
            SQLiteDatabase db = mDbHelper.getWritableDatabase();

            // New value for one column
            ContentValues values = new ContentValues();
            values.put(SensorConfigContract.SensorEntry.COLUMN_NAME_VISIBLE, mVisible ? 1 : 0);

            // Which row to update, based on the ID
            String selection = SensorConfigContract.SensorEntry._ID + "=?";
            String[] selectionArgs = {String.valueOf(mId)};

            int count = db.update(
                    SensorConfigContract.SensorEntry.TABLE_NAME,
                    values,
                    selection,
                    selectionArgs);
            db.close();
        }
    }

    private Double getMeasure(final String key) {
        try {
            return (!measureHasExpired() && mData.has(key)) ? mData.getDouble(key) : null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean measureHasExpired() {
        long currentTime = System.currentTimeMillis();
        // Measure expired?
        return mLastUpdate + mpMeasureExpirationTime < currentTime;
    }

    public Integer getThermostatTemperature()
    {
        try {
            if (mData.has("thermostat")) {
                JSONObject thermostat = mData.getJSONObject("thermostat");
                return thermostat.has("temperature") ? thermostat.getInt("temperature") : null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setThermostatTemperature(Integer temperature)
    {
        try {
            if (mData.has("thermostat")) {
                JSONObject thermostat = mData.getJSONObject("thermostat");
                thermostat.put("temperature", temperature);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ThermostatMode getThermostatMode()
    {
        try {
            if (mData.has("thermostat")) {
                JSONObject thermostat = mData.getJSONObject("thermostat");
                switch (thermostat.getString("mode")) {
                    case "HEAT":
                        return ThermostatMode.HEAT;
                    case "COOL":
                        return ThermostatMode.COOL;
                    case "OFF":
                    default:
                        return ThermostatMode.OFF;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ThermostatMode.OFF;
    }

    public void setThermostatMode(ThermostatMode thermostatMode)
    {
        try {
            if (mData.has("thermostat")) {
                JSONObject thermostat = mData.getJSONObject("thermostat");
                switch (thermostatMode) {
                    case HEAT:
                        thermostat.put("mode", "HEAT");
                    case COOL:
                        thermostat.put("mode", "COOL");
                    case OFF:
                    default:
                        thermostat.put("mode", "OFF");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Double getHumidity() {
        return getMeasure("humidity");
    }

    public Double getTemperature() {
        return getMeasure("temperature");
    }

    public enum ThermostatMode {
        OFF, HEAT, COOL
    }

    static public ThermostatMode ModeFromString(String mode) {
        switch (mode.toUpperCase()) {
            case "HEAT":
                return ThermostatMode.HEAT;
            case "COOL":
                return ThermostatMode.COOL;
            case "OFF":
                return ThermostatMode.OFF;
        }
        return ThermostatMode.OFF;
    }

    static public String ModeToString(ThermostatMode mode) {
        switch (mode) {
            case OFF:
                return "OFF";
            case HEAT:
                return "HEAT";
            case COOL:
                return "COOL";
        }
        return "OFF";
    }

}
