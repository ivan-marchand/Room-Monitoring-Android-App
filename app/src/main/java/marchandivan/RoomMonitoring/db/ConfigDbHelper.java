package marchandivan.RoomMonitoring.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by ivan on 10/26/15.
 */

public class ConfigDbHelper extends SQLiteOpenHelper {
    private static final String SQL_CREATE_SENSOR_ENTRIES =
            "CREATE TABLE " + SensorConfigContract.SensorEntry.TABLE_NAME + " (" +
                    SensorConfigContract.SensorEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    SensorConfigContract.SensorEntry.COLUMN_NAME_SENSOR + " TEXT," +
                    SensorConfigContract.SensorEntry.COLUMN_NAME_TYPE + " TEXT," +
                    SensorConfigContract.SensorEntry.COLUMN_NAME_LAST_UPDATE + " INTEGER," +
                    SensorConfigContract.SensorEntry.COLUMN_NAME_LAST_ALARM + " INTEGER," +
                    SensorConfigContract.SensorEntry.COLUMN_NAME_DATA + " TEXT," +
                    SensorConfigContract.SensorEntry.COLUMN_NAME_DEVICE + " INTEGER," +
                    SensorConfigContract.SensorEntry.COLUMN_NAME_CONFIG + " TEXT" +
                    ")";

    private static final String SQL_CREATE_DEVICE_ENTRIES =
            "CREATE TABLE " + DeviceConfigContract.DeviceEntry.TABLE_NAME + " (" +
                    DeviceConfigContract.DeviceEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    DeviceConfigContract.DeviceEntry.COLUMN_NAME_DEVICE + " TEXT," +
                    DeviceConfigContract.DeviceEntry.COLUMN_NAME_TYPE + " TEXT," +
                    DeviceConfigContract.DeviceEntry.COLUMN_NAME_HTTPS + " INTEGER," +
                    DeviceConfigContract.DeviceEntry.COLUMN_NAME_HOST + " TEXT," +
                    DeviceConfigContract.DeviceEntry.COLUMN_NAME_PORT + " INTEGER," +
                    DeviceConfigContract.DeviceEntry.COLUMN_NAME_AUTH_CONFIG + " TEXT" +
                    ")";

    private static final String SQL_CREATE_ALARM_ENTRIES =
            "CREATE TABLE " + AlarmConfigContract.AlarmEntry.TABLE_NAME + " (" +
                    AlarmConfigContract.AlarmEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    AlarmConfigContract.AlarmEntry.COLUMN_NAME_SENSOR + " INTEGER," +
                    AlarmConfigContract.AlarmEntry.COLUMN_NAME_ALARM_ACTIVE + " INTEGER, " +
                    AlarmConfigContract.AlarmEntry.COLUMN_NAME_MAX_TEMP + " INTEGER," +
                    AlarmConfigContract.AlarmEntry.COLUMN_NAME_MIN_TEMP + " INTEGER," +
                    AlarmConfigContract.AlarmEntry.COLUMN_NAME_START_HOUR + " INTEGER," +
                    AlarmConfigContract.AlarmEntry.COLUMN_NAME_START_MINUTE + " INTEGER," +
                    AlarmConfigContract.AlarmEntry.COLUMN_NAME_STOP_HOUR + " INTEGER," +
                    AlarmConfigContract.AlarmEntry.COLUMN_NAME_STOP_MINUTE + " INTEGER" +
                    ")";

    private static final String SQL_DELETE_SENSOR_ENTRIES =
            "DROP TABLE IF EXISTS " + SensorConfigContract.SensorEntry.TABLE_NAME;
    private static final String SQL_DELETE_DEVICE_ENTRIES =
            "DROP TABLE IF EXISTS " + DeviceConfigContract.DeviceEntry.TABLE_NAME;
    private static final String SQL_DELETE_ALARM_ENTRIES =
            "DROP TABLE IF EXISTS " + AlarmConfigContract.AlarmEntry.TABLE_NAME;

    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "Config.db";

    public ConfigDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_SENSOR_ENTRIES);
        db.execSQL(SQL_CREATE_DEVICE_ENTRIES);
        db.execSQL(SQL_CREATE_ALARM_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_SENSOR_ENTRIES);
        db.execSQL(SQL_DELETE_DEVICE_ENTRIES);
        db.execSQL(SQL_DELETE_ALARM_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
