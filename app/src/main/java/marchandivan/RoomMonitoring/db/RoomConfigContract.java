package marchandivan.RoomMonitoring.db;

import android.provider.BaseColumns;

/**
 * Created by ivan on 10/26/15.
 */
public class RoomConfigContract {
    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public RoomConfigContract() {}

    /* Inner class that defines the table contents */
    public static abstract class RoomEntry implements BaseColumns {
        public static final String TABLE_NAME = "room_config";
        public static final String COLUMN_NAME_ROOM = "room";
        public static final String COLUMN_NAME_LAST_UPDATE = "last_update";
        public static final String COLUMN_NAME_LAST_ALARM = "last_alarm";
    }
}
