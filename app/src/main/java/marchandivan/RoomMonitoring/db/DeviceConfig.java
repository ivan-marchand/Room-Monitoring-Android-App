package marchandivan.RoomMonitoring.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by ivan on 9/8/16.
 */
public class DeviceConfig {
    private long mId = 0;
    private ConfigDbHelper mDbHelper;
    private String mDeviceName;
    private String mType;
    private Boolean mHttps;
    private String mHost;
    private Integer mPort;
    private String mBasePath;
    private JSONObject mAuthConfig = new JSONObject();


    public final long getId() {
        return mId;
    }

    public final String getName() {
        return mDeviceName;
    }

    public final String getType() {
        return mType;
    }

    public boolean isHttps() {
        return mHttps;
    }

    public final String getHost() {
        return mHost;
    }

    public int getPort() {
        return mPort;
    }

    public final String getBasePath() {
        return mBasePath;
    }

    public String toString()
    {
        return String.format("%s (%d)", mDeviceName, mId);
    }

    public void setUserPassword(final String user, final String password) {
        try {
            mAuthConfig.put("user", user);
            mAuthConfig.put("password", password);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public final String getUser() {
        return getAuthField("user");
    }

    public final String getPassword() {
        return getAuthField("password");
    }

    private final String getAuthField(String field) {
        try {
            return mAuthConfig.has(field) ? mAuthConfig.getString(field) : null;
        } catch (Exception e) {
            return null;
        }
    }

    static public HashMap<Long, DeviceConfig> GetMap(Context context) {
        HashMap<Long, DeviceConfig> deviceConfigs = new HashMap<Long, DeviceConfig>();
        ConfigDbHelper dbHelper = new ConfigDbHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] projection = {
                DeviceConfigContract.DeviceEntry._ID,
                DeviceConfigContract.DeviceEntry.COLUMN_NAME_DEVICE,
        };

        Cursor cursor = db.query(
                DeviceConfigContract.DeviceEntry.TABLE_NAME,  // The table to query
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
            Long id = cursor.getLong(cursor.getColumnIndexOrThrow(DeviceConfigContract.DeviceEntry._ID));
            Log.d("DeviceConfigFragment:readList", "Device " + id);
            DeviceConfig deviceConfig = new DeviceConfig(context, id);
            if (deviceConfig.read()) {
                deviceConfigs.put(id, deviceConfig);
            }
            cursor.moveToNext();
        }

        db.close();
        return deviceConfigs;

    }

    public DeviceConfig(Context context, long id) {
        mId = id;
        mDbHelper = new ConfigDbHelper(context);
    }

    public DeviceConfig(Context context, String deviceName, String type) {
        mDbHelper = new ConfigDbHelper(context);
        mDeviceName = deviceName;
        mType = type;
        mHttps = null;
        mHost = null;
        mPort = null;
        mAuthConfig = new JSONObject();
    }

    public DeviceConfig(Context context, String deviceName, String type, boolean https, String host, int port, String basePath) {
        mDbHelper = new ConfigDbHelper(context);
        mDeviceName = deviceName;
        mType = type;
        mHttps = https;
        mHost = host;
        mPort = port;
        mBasePath = basePath;
        mAuthConfig = new JSONObject();
    }

    public void add() {

        // Gets the data repository in wyrite mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(DeviceConfigContract.DeviceEntry.COLUMN_NAME_DEVICE, mDeviceName);
        values.put(DeviceConfigContract.DeviceEntry.COLUMN_NAME_TYPE, mType);
        values.put(DeviceConfigContract.DeviceEntry.COLUMN_NAME_HTTPS, mHttps);
        values.put(DeviceConfigContract.DeviceEntry.COLUMN_NAME_HOST, mHost);
        values.put(DeviceConfigContract.DeviceEntry.COLUMN_NAME_PORT, mPort);
        values.put(DeviceConfigContract.DeviceEntry.COLUMN_NAME_PATH, mBasePath);
        values.put(DeviceConfigContract.DeviceEntry.COLUMN_NAME_AUTH_CONFIG, mAuthConfig.toString());

        // Insert the new row, returning the primary key value of the new row
        mId = db.insert(
                DeviceConfigContract.DeviceEntry.TABLE_NAME,
                null,
                values);
        db.close();
    }

    public void delete() {
        // Gets the data repository in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        // Define 'where' part of query.
        String selection = DeviceConfigContract.DeviceEntry._ID + " =?";
        // Specify arguments in placeholder order.
        String[] selectionArgs = {String.valueOf(mId)};
        // Issue SQL statement.
        db.delete(DeviceConfigContract.DeviceEntry.TABLE_NAME, selection, selectionArgs);
        db.close();
    }

    public boolean read() {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        String[] projection = {
                DeviceConfigContract.DeviceEntry.COLUMN_NAME_DEVICE,
                DeviceConfigContract.DeviceEntry.COLUMN_NAME_TYPE,
                DeviceConfigContract.DeviceEntry.COLUMN_NAME_HTTPS,
                DeviceConfigContract.DeviceEntry.COLUMN_NAME_HOST,
                DeviceConfigContract.DeviceEntry.COLUMN_NAME_PORT,
                DeviceConfigContract.DeviceEntry.COLUMN_NAME_PATH,
                DeviceConfigContract.DeviceEntry.COLUMN_NAME_AUTH_CONFIG
        };

        String[] selectionArgs = {String.valueOf(mId)};
        Cursor cursor = db.query(
                DeviceConfigContract.DeviceEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                DeviceConfigContract.DeviceEntry._ID + "=?",         // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                      // The sort order
        );

        // Anything found
        if (cursor.getCount() == 0) {
            Log.d("DeviceConfigFragment", "Device not found!");
            db.close();
            return false;
        }

        // Get values
        cursor.moveToFirst();
        mDeviceName = cursor.getString(cursor.getColumnIndexOrThrow(DeviceConfigContract.DeviceEntry.COLUMN_NAME_DEVICE));
        mType = cursor.getString(cursor.getColumnIndexOrThrow(DeviceConfigContract.DeviceEntry.COLUMN_NAME_TYPE));
        mHttps = (cursor.getInt(cursor.getColumnIndexOrThrow(DeviceConfigContract.DeviceEntry.COLUMN_NAME_HTTPS)) > 0);
        mHost = cursor.getString(cursor.getColumnIndexOrThrow(DeviceConfigContract.DeviceEntry.COLUMN_NAME_HOST));
        mPort = cursor.getInt(cursor.getColumnIndexOrThrow(DeviceConfigContract.DeviceEntry.COLUMN_NAME_PORT));
        mBasePath = cursor.getString(cursor.getColumnIndexOrThrow(DeviceConfigContract.DeviceEntry.COLUMN_NAME_PATH));
        try {
            mAuthConfig = new JSONObject(cursor.getString(cursor.getColumnIndexOrThrow(DeviceConfigContract.DeviceEntry.COLUMN_NAME_AUTH_CONFIG)));
        } catch (JSONException e) {
            // Unable to decode
            mAuthConfig = new JSONObject();
        }

        db.close();
        return true;
    }

}