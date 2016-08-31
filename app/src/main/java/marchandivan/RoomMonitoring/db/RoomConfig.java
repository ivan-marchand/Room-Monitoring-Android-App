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
 * Created by ivan on 10/26/15.
 */
public class RoomConfig {
    // Cache expiration time
    static final long mExpirationTime = 30 * 60 * 1000; // 30 minutes

    private long mId = 0;
    private ConfigDbHelper mDbHelper;
    public String mRoomName;
    public boolean mVisible = false;
    public long mLastUpdate = 0;
    public long mLastAlarm = 0;
    public JSONObject mData = new JSONObject();

    static public HashMap<String, RoomConfig> GetMap(Context context) {
        HashMap<String, RoomConfig> roomConfigs = new HashMap<String, RoomConfig>();
        ConfigDbHelper dbHelper = new ConfigDbHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] projection = {
                RoomConfigContract.RoomEntry._ID,
                RoomConfigContract.RoomEntry.COLUMN_NAME_ROOM,
        };

        Cursor cursor = db.query(
                RoomConfigContract.RoomEntry.TABLE_NAME,  // The table to query
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
            String roomName = cursor.getString(cursor.getColumnIndexOrThrow(RoomConfigContract.RoomEntry.COLUMN_NAME_ROOM));
            Log.d("RoomConfig:readList", "Room " + roomName);
            RoomConfig roomConfig = new RoomConfig(context, roomName);
            if (roomConfig.read()) {
                roomConfigs.put(roomName, roomConfig);
            }
            cursor.moveToNext();
        }

        db.close();
        return roomConfigs;

    }
    public RoomConfig(Context context, String roomName) {
        mRoomName = roomName;
        mDbHelper = new ConfigDbHelper(context);
    }

    public RoomConfig(Context context, String roomName, long lastUpdate, JSONObject data) {
        mRoomName = roomName;
        mDbHelper = new ConfigDbHelper(context);
        mLastUpdate = lastUpdate;
        mData = data;
    }

    public void add() {
        // Delete existing entry if any
        delete();

        // Gets the data repository in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(RoomConfigContract.RoomEntry.COLUMN_NAME_ROOM, mRoomName);
        values.put(RoomConfigContract.RoomEntry.COLUMN_NAME_VISIBLE, mVisible ? 1 : 0);
        values.put(RoomConfigContract.RoomEntry.COLUMN_NAME_LAST_UPDATE, mLastUpdate);
        values.put(RoomConfigContract.RoomEntry.COLUMN_NAME_LAST_ALARM, mLastAlarm);
        values.put(RoomConfigContract.RoomEntry.COLUMN_NAME_DATA, mData.toString());

        // Insert the new row, returning the primary key value of the new row
        mId = db.insert(
                RoomConfigContract.RoomEntry.TABLE_NAME,
                null,
                values);
        db.close();
    }

    public void delete() {
        // Gets the data repository in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        // Define 'where' part of query.
        String selection = RoomConfigContract.RoomEntry.COLUMN_NAME_ROOM + " =?";
        // Specify arguments in placeholder order.
        String[] selectionArgs = { mRoomName };
        // Issue SQL statement.
        db.delete(RoomConfigContract.RoomEntry.TABLE_NAME, selection, selectionArgs);
        db.close();
    }

    public boolean read() {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        String[] projection = {
                RoomConfigContract.RoomEntry._ID,
                RoomConfigContract.RoomEntry.COLUMN_NAME_VISIBLE,
                RoomConfigContract.RoomEntry.COLUMN_NAME_LAST_UPDATE,
                RoomConfigContract.RoomEntry.COLUMN_NAME_LAST_ALARM,
                RoomConfigContract.RoomEntry.COLUMN_NAME_DATA
        };

        String[] selectionArgs = {mRoomName};
        Cursor cursor = db.query(
                RoomConfigContract.RoomEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                RoomConfigContract.RoomEntry.COLUMN_NAME_ROOM + "=?",         // The columns for the WHERE clause
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
        mId = cursor.getLong(cursor.getColumnIndexOrThrow(RoomConfigContract.RoomEntry._ID));
        mVisible = cursor.getInt(cursor.getColumnIndexOrThrow(RoomConfigContract.RoomEntry.COLUMN_NAME_VISIBLE)) > 0;
        mLastUpdate = cursor.getLong(cursor.getColumnIndexOrThrow(RoomConfigContract.RoomEntry.COLUMN_NAME_LAST_UPDATE));
        mLastAlarm = cursor.getLong(cursor.getColumnIndexOrThrow(RoomConfigContract.RoomEntry.COLUMN_NAME_LAST_ALARM));
        try {
            mData = new JSONObject(cursor.getString(cursor.getColumnIndexOrThrow(RoomConfigContract.RoomEntry.COLUMN_NAME_DATA)));
        } catch (JSONException e) {
            // Unable to decode
            mData = new JSONObject();
        }

        db.close();
        return true;
    }

    public void update(long lastUpdate, JSONObject data) {
        mLastUpdate = lastUpdate;
        mData = data;

        // Gets the data repository in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // New value for one column
        ContentValues values = new ContentValues();
        values.put(RoomConfigContract.RoomEntry.COLUMN_NAME_LAST_UPDATE, mLastUpdate);
        values.put(RoomConfigContract.RoomEntry.COLUMN_NAME_DATA, mData.toString());

        // Which row to update, based on the ID
        String selection = RoomConfigContract.RoomEntry.COLUMN_NAME_ROOM + "=?";
        String[] selectionArgs = { mRoomName };

        int count = db.update(
                RoomConfigContract.RoomEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs);
        db.close();
    }

    public void setVisibility(boolean isVisible) {
        mVisible = isVisible;

        // Gets the data repository in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // New value for one column
        ContentValues values = new ContentValues();
        values.put(RoomConfigContract.RoomEntry.COLUMN_NAME_VISIBLE, mVisible ? 1 : 0);

        // Which row to update, based on the ID
        String selection = RoomConfigContract.RoomEntry.COLUMN_NAME_ROOM + "=?";
        String[] selectionArgs = { mRoomName };

        int count = db.update(
                RoomConfigContract.RoomEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs);
        db.close();
    }

    public void updateAlarm() {
        mLastAlarm = System.currentTimeMillis();
        // Gets the data repository in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // New value for one column
        ContentValues values = new ContentValues();
        values.put(RoomConfigContract.RoomEntry.COLUMN_NAME_LAST_ALARM, mLastAlarm);

        // Which row to update, based on the ID
        String selection = RoomConfigContract.RoomEntry.COLUMN_NAME_ROOM + "=?";
        String[] selectionArgs = { mRoomName };

        int count = db.update(
                RoomConfigContract.RoomEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs);
        db.close();
    }

    public boolean hasExpired() {
        long currentTime = System.currentTimeMillis();
        // Measure expired?
        return mLastUpdate + mExpirationTime > currentTime;
    }
}
