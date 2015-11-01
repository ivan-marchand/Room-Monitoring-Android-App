package marchandivan.RoomMonitoring.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by ivan on 10/26/15.
 */

public class ConfigDbHelper extends SQLiteOpenHelper {
    private static final String SQL_CREATE_ROOM_ENTRIES =
            "CREATE TABLE " + RoomConfigContract.RoomEntry.TABLE_NAME + " (" +
                    RoomConfigContract.RoomEntry._ID + " INTEGER PRIMARY KEY," +
                    RoomConfigContract.RoomEntry.COLUMN_NAME_ROOM + " TEXT," +
                    RoomConfigContract.RoomEntry.COLUMN_NAME_LAST_UPDATE + " INTEGER," +
                    RoomConfigContract.RoomEntry.COLUMN_NAME_ALARM_ACTIVE + " INTEGER, " +
                    RoomConfigContract.RoomEntry.COLUMN_NAME_LAST_ALARM + " INTEGER" +
                    ")";

    private static final String SQL_CREATE_ALARM_ENTRIES =
            "CREATE TABLE " + AlarmConfigContract.AlarmEntry.TABLE_NAME + " (" +
                    AlarmConfigContract.AlarmEntry._ID + " INTEGER PRIMARY KEY," +
                    AlarmConfigContract.AlarmEntry.COLUMN_NAME_ROOM + " TEXT," +
                    AlarmConfigContract.AlarmEntry.COLUMN_NAME_MAX_TEMP + " INTEGER," +
                    AlarmConfigContract.AlarmEntry.COLUMN_NAME_MIN_TEMP + " INTEGER," +
                    AlarmConfigContract.AlarmEntry.COLUMN_NAME_START_HOUR + " INTEGER," +
                    AlarmConfigContract.AlarmEntry.COLUMN_NAME_START_MINUTE + " INTEGER," +
                    AlarmConfigContract.AlarmEntry.COLUMN_NAME_STOP_HOUR + " INTEGER," +
                    AlarmConfigContract.AlarmEntry.COLUMN_NAME_STOP_MINUTE + " INTEGER" +
                    ")";

    private static final String SQL_DELETE_ROOM_ENTRIES =
            "DROP TABLE IF EXISTS " + RoomConfigContract.RoomEntry.TABLE_NAME;
    private static final String SQL_DELETE_ALARM_ENTRIES =
            "DROP TABLE IF EXISTS " + AlarmConfigContract.AlarmEntry.TABLE_NAME;

    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "RoomConfig.db";

    public ConfigDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ROOM_ENTRIES);
        db.execSQL(SQL_CREATE_ALARM_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ROOM_ENTRIES);
        db.execSQL(SQL_DELETE_ALARM_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
