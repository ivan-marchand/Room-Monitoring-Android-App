package marchandivan.RoomMonitoring.db;

import android.provider.BaseColumns;

/**
 * Created by ivan on 10/31/15.
 */
public class AlarmConfigContract {
    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public AlarmConfigContract() {}

    /* Inner class that defines the table contents */
    public static abstract class AlarmEntry implements BaseColumns {
        public static final String TABLE_NAME = "alarm_config";
        public static final String COLUMN_NAME_SENSOR = "sensor";
        public static final String COLUMN_NAME_ALARM_ACTIVE = "alarm_active";
        public static final String COLUMN_NAME_MAX_TEMP = "max_temp";
        public static final String COLUMN_NAME_MIN_TEMP = "min_temp";
        public static final String COLUMN_NAME_START_HOUR = "start_hour";
        public static final String COLUMN_NAME_START_MINUTE = "start_minute";
        public static final String COLUMN_NAME_STOP_HOUR = "stop_hour";
        public static final String COLUMN_NAME_STOP_MINUTE = "stop_minute";
    }
}
