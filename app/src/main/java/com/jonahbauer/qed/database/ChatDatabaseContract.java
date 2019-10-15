package com.jonahbauer.qed.database;

import android.provider.BaseColumns;

public final class ChatDatabaseContract {
    private ChatDatabaseContract() {}

    public static class ChatEntry implements BaseColumns {
        public static final String TABLE_NAME = "chatlog";
        public static final String COLUMN_NAME_ID = "messageId";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_USERNAME = "username";
        public static final String COLUMN_NAME_USERID = "userid";
        public static final String COLUMN_NAME_COLOR = "color";
        public static final String COLUMN_NAME_MESSAGE = "message";
        public static final String COLUMN_NAME_DATE = "date";
        public static final String COLUMN_NAME_BOTTAG = "bottag";
        public static final String COLUMN_NAME_CHANNEL = "channel";
    }
}
