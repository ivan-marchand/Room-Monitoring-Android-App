package marchandivan.RoomMonitoring.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;

/**
 * Created by ivan on 10/31/15.
 */
public class AlarmConfig {

    public class Alarm {
        public long mId = 0;
        public Boolean mAlarmActive = false;
        public Integer mMaxTemp = 0;
        public Integer mMinTemp = 0;
        public Pair<Integer, Integer> mStartTime = new Pair<Integer, Integer>(0, 0);
        public Pair<Integer, Integer> mStopTime = new Pair<Integer, Integer>(0, 0);

        private Alarm() {}
        private Alarm(Integer minTemp,
                      Integer maxTemp,
                      Pair<Integer, Integer> startTime,
                      Pair<Integer, Integer> stopTime) {
            mMinTemp = minTemp;
            mMaxTemp = maxTemp;
            mStartTime = startTime;
            mStopTime = stopTime;

        }

        public boolean isActiveAnyTime() {
            return mStartTime.first.equals(mStopTime.first) && mStartTime.second.equals(mStopTime.second) && mStartTime.first.equals(0) && mStartTime.second.equals(0);
        }

    }

    private ConfigDbHelper mDbHelper;
    public String mRoomName;

    public Alarm getAlarmInstance() {
        return new Alarm();
    }

    public AlarmConfig(Context context, String roomName) {
        mRoomName = roomName;
        mDbHelper = new ConfigDbHelper(context);
    }

    public void add(Integer minTemp,
                    Integer maxTemp,
                    Pair<Integer, Integer> startTime,
                    Pair<Integer, Integer> stopTime) {

        Alarm alarm = new Alarm(minTemp, maxTemp, startTime, stopTime);
        update(alarm);
    }

    public void update(Alarm alarm) {

        // Entry already exists ?
        if (alarm.mId != 0) {
            delete(alarm);
        }

        // Gets the data repository in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(AlarmConfigContract.AlarmEntry.COLUMN_NAME_ROOM, mRoomName);
        values.put(AlarmConfigContract.AlarmEntry.COLUMN_NAME_ALARM_ACTIVE, alarm.mAlarmActive ? 1:0);
        values.put(AlarmConfigContract.AlarmEntry.COLUMN_NAME_MAX_TEMP, alarm.mMaxTemp);
        values.put(AlarmConfigContract.AlarmEntry.COLUMN_NAME_MIN_TEMP, alarm.mMinTemp);
        values.put(AlarmConfigContract.AlarmEntry.COLUMN_NAME_START_HOUR, alarm.mStartTime.first);
        values.put(AlarmConfigContract.AlarmEntry.COLUMN_NAME_START_MINUTE, alarm.mStartTime.second);
        values.put(AlarmConfigContract.AlarmEntry.COLUMN_NAME_STOP_HOUR, alarm.mStopTime.first);
        values.put(AlarmConfigContract.AlarmEntry.COLUMN_NAME_STOP_MINUTE, alarm.mStopTime.second);

        // Insert the new row, returning the primary key value of the new row
        alarm.mId = db.insert(
                AlarmConfigContract.AlarmEntry.TABLE_NAME,
                null,
                values);
        db.close();
    }

    public void delete() {
        Log.d("AlarmConfig:delete", "Room " + mRoomName);
        // Gets the data repository in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        // Define 'where' part of query.
        String selection = AlarmConfigContract.AlarmEntry.COLUMN_NAME_ROOM + " =?";
        // Specify arguments in placeholder order.
        String[] selectionArgs = { mRoomName };
        // Issue SQL statement.
        db.delete(AlarmConfigContract.AlarmEntry.TABLE_NAME, selection, selectionArgs);
        db.close();
    }

    public void delete(Alarm alarm) {
        Log.d("AlarmConfig:delete", "Alarm " + alarm.mId);
        // Gets the data repository in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        // Define 'where' part of query.
        String selection = AlarmConfigContract.AlarmEntry._ID + " =?";
        // Specify arguments in placeholder order.
        String[] selectionArgs = { String.valueOf(alarm.mId) };
        // Issue SQL statement.
        db.delete(AlarmConfigContract.AlarmEntry.TABLE_NAME, selection, selectionArgs);
        db.close();
    }

    public ArrayList<Alarm> read() {
        ArrayList<Alarm> alarms = new ArrayList<Alarm>();

        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        String[] projection = {
                AlarmConfigContract.AlarmEntry._ID,
                AlarmConfigContract.AlarmEntry.COLUMN_NAME_ALARM_ACTIVE,
                AlarmConfigContract.AlarmEntry.COLUMN_NAME_MAX_TEMP,
                AlarmConfigContract.AlarmEntry.COLUMN_NAME_MIN_TEMP,
                AlarmConfigContract.AlarmEntry.COLUMN_NAME_START_HOUR,
                AlarmConfigContract.AlarmEntry.COLUMN_NAME_START_MINUTE,
                AlarmConfigContract.AlarmEntry.COLUMN_NAME_STOP_HOUR,
                AlarmConfigContract.AlarmEntry.COLUMN_NAME_STOP_MINUTE,
        };

        String[] selectionArgs = {mRoomName};
        Cursor cursor = db.query(
                AlarmConfigContract.AlarmEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                AlarmConfigContract.AlarmEntry.COLUMN_NAME_ROOM + "=?",         // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                      // The sort order
        );

        // Anything found
        if (cursor.getCount() == 0) {
            db.close();
            return alarms;
        }

        // Get values
        while (cursor.moveToNext()) {
            Alarm alarm = new Alarm();
            alarm.mId = cursor.getLong(cursor.getColumnIndexOrThrow(AlarmConfigContract.AlarmEntry._ID));
            alarm.mAlarmActive = cursor.getInt(cursor.getColumnIndexOrThrow(AlarmConfigContract.AlarmEntry.COLUMN_NAME_ALARM_ACTIVE)) == 1;
            alarm.mMaxTemp = cursor.getInt(cursor.getColumnIndexOrThrow(AlarmConfigContract.AlarmEntry.COLUMN_NAME_MAX_TEMP));
            alarm.mMinTemp = cursor.getInt(cursor.getColumnIndexOrThrow(AlarmConfigContract.AlarmEntry.COLUMN_NAME_MIN_TEMP));
            Integer startHour = cursor.getInt(cursor.getColumnIndexOrThrow(AlarmConfigContract.AlarmEntry.COLUMN_NAME_START_HOUR));
            Integer startMinute = cursor.getInt(cursor.getColumnIndexOrThrow(AlarmConfigContract.AlarmEntry.COLUMN_NAME_START_MINUTE));
            alarm.mStartTime = new Pair<Integer, Integer>(startHour, startMinute);
            Integer stopHour = cursor.getInt(cursor.getColumnIndexOrThrow(AlarmConfigContract.AlarmEntry.COLUMN_NAME_STOP_HOUR));
            Integer stopMinute = cursor.getInt(cursor.getColumnIndexOrThrow(AlarmConfigContract.AlarmEntry.COLUMN_NAME_STOP_MINUTE));
            alarm.mStopTime = new Pair<Integer, Integer>(stopHour, stopMinute);

            alarms.add(alarm);
        }


        db.close();
        return alarms;
    }

    public Alarm read(long alarmId) {

        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        String[] projection = {
                AlarmConfigContract.AlarmEntry._ID,
                AlarmConfigContract.AlarmEntry.COLUMN_NAME_ALARM_ACTIVE,
                AlarmConfigContract.AlarmEntry.COLUMN_NAME_MAX_TEMP,
                AlarmConfigContract.AlarmEntry.COLUMN_NAME_MIN_TEMP,
                AlarmConfigContract.AlarmEntry.COLUMN_NAME_START_HOUR,
                AlarmConfigContract.AlarmEntry.COLUMN_NAME_START_MINUTE,
                AlarmConfigContract.AlarmEntry.COLUMN_NAME_STOP_HOUR,
                AlarmConfigContract.AlarmEntry.COLUMN_NAME_STOP_MINUTE,
        };

        String[] selectionArgs = {String.valueOf(alarmId)};
        Cursor cursor = db.query(
                AlarmConfigContract.AlarmEntry.TABLE_NAME, // The table to query
                projection,                                // The columns to return
                AlarmConfigContract.AlarmEntry._ID + "=?", // The columns for the WHERE clause
                selectionArgs,                             // The values for the WHERE clause
                null,                                      // don't group the rows
                null,                                      // don't filter by row groups
                null                                       // The sort order
        );

        // Anything found
        if (cursor.getCount() == 0) {
            db.close();
            return null;
        }

        // Get values
        Alarm alarm = new Alarm();

        cursor.moveToFirst();
        alarm.mId = cursor.getLong(cursor.getColumnIndexOrThrow(AlarmConfigContract.AlarmEntry._ID));
        alarm.mAlarmActive = cursor.getInt(cursor.getColumnIndexOrThrow(AlarmConfigContract.AlarmEntry.COLUMN_NAME_ALARM_ACTIVE)) == 1;
        alarm.mMaxTemp = cursor.getInt(cursor.getColumnIndexOrThrow(AlarmConfigContract.AlarmEntry.COLUMN_NAME_MAX_TEMP));
        alarm.mMinTemp = cursor.getInt(cursor.getColumnIndexOrThrow(AlarmConfigContract.AlarmEntry.COLUMN_NAME_MIN_TEMP));
        Integer startHour = cursor.getInt(cursor.getColumnIndexOrThrow(AlarmConfigContract.AlarmEntry.COLUMN_NAME_START_HOUR));
        Integer startMinute = cursor.getInt(cursor.getColumnIndexOrThrow(AlarmConfigContract.AlarmEntry.COLUMN_NAME_START_MINUTE));
        alarm.mStartTime = new Pair<Integer, Integer>(startHour, startMinute);
        Integer stopHour = cursor.getInt(cursor.getColumnIndexOrThrow(AlarmConfigContract.AlarmEntry.COLUMN_NAME_STOP_HOUR));
        Integer stopMinute = cursor.getInt(cursor.getColumnIndexOrThrow(AlarmConfigContract.AlarmEntry.COLUMN_NAME_STOP_MINUTE));
        alarm.mStopTime = new Pair<Integer, Integer>(stopHour, stopMinute);

        db.close();
        return alarm;
    }

}
