package marchandivan.RoomMonitoring.db;

import android.provider.BaseColumns;

/**
 * Created by ivan on 9/8/16.
 */
public class DeviceConfigContract {
    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public DeviceConfigContract() {}

    /* Inner class that defines the table contents */
    public static abstract class DeviceEntry implements BaseColumns {
        public static final String TABLE_NAME = "device_config";
        public static final String COLUMN_NAME_DEVICE = "device";
        public static final String COLUMN_NAME_TYPE = "type";
        public static final String COLUMN_NAME_HTTPS = "https";
        public static final String COLUMN_NAME_HOST = "host";
        public static final String COLUMN_NAME_PORT = "port";
        public static final String COLUMN_NAME_AUTH_CONFIG = "auth_config";
    }
}
