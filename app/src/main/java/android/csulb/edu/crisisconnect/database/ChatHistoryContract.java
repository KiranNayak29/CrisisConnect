package android.csulb.edu.crisisconnect.database;

import android.provider.BaseColumns;

/**
 * Created by vaibhavjain on 5/8/2017
 */

public class ChatHistoryContract {

    private ChatHistoryContract() {
    }

    public static class Messages implements BaseColumns {
        public static final String TABLE_NAME = "Messages";
        //public static final String COLUMN_NAME_ID = "_id";
        public static final String COLUMN_NAME_SENDER = "sender";
        public static final String COLUMN_NAME_RECEIVER = "receiver";
        public static final String COLUMN_NAME_MESSAGE_TYPE = "message_type";
        public static final String COLUMN_NAME_MESSAGE = "message";
        public static final String COLUMN_NAME_LATITUDE = "latitude";
        public static final String COLUMN_NAME_LONGITUDE = "longitude";
    }
}
