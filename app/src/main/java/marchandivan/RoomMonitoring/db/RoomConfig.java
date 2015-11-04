package marchandivan.RoomMonitoring.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.HashMap;

/**
 * Created by ivan on 10/26/15.
 */
public class RoomConfig {
    private long mId = 0;
    private ConfigDbHelper mDbHelper;
    public String mRoomName;
    public long mLastUpdate = 0;
    public long mLastAlarm = 0;

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

    public void update() {
        // Delete existing entry if any
        delete();

        // Gets the data repository in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(RoomConfigContract.RoomEntry.COLUMN_NAME_ROOM, mRoomName);
        values.put(RoomConfigContract.RoomEntry.COLUMN_NAME_LAST_UPDATE, mLastUpdate);
        values.put(RoomConfigContract.RoomEntry.COLUMN_NAME_LAST_ALARM, mLastAlarm);

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
                RoomConfigContract.RoomEntry.COLUMN_NAME_LAST_UPDATE,
                RoomConfigContract.RoomEntry.COLUMN_NAME_LAST_ALARM
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
        mLastUpdate = cursor.getLong(cursor.getColumnIndexOrThrow(RoomConfigContract.RoomEntry.COLUMN_NAME_LAST_UPDATE));
        mLastAlarm = cursor.getLong(cursor.getColumnIndexOrThrow(RoomConfigContract.RoomEntry.COLUMN_NAME_LAST_ALARM));

        db.close();
        return true;
    }

    public void update(long lastUpdate) {
        mLastUpdate = lastUpdate;
        // Gets the data repository in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // New value for one column
        ContentValues values = new ContentValues();
        values.put(RoomConfigContract.RoomEntry.COLUMN_NAME_LAST_UPDATE, mLastUpdate);

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

}
