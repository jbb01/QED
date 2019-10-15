package com.jonahbauer.qed.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static com.jonahbauer.qed.database.ChatDatabaseContract.ChatEntry;

class ChatDatabaseHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 8;
    private static final String DATABASE_NAME = "chatLog.db";

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + ChatEntry.TABLE_NAME + " (" +
                    ChatEntry.COLUMN_NAME_ID + " INTEGER PRIMARY KEY," +
                    ChatEntry.COLUMN_NAME_USERNAME + " TEXT," +
                    ChatEntry.COLUMN_NAME_NAME + " TEXT," +
                    ChatEntry.COLUMN_NAME_USERID + " INTEGER," +
                    ChatEntry.COLUMN_NAME_COLOR + " TEXT," +
                    ChatEntry.COLUMN_NAME_MESSAGE + " TEXT," +
                    ChatEntry.COLUMN_NAME_DATE + " DATETIME," +
                    ChatEntry.COLUMN_NAME_CHANNEL + " TEXT," +
                    ChatEntry.COLUMN_NAME_BOTTAG + " INTEGER)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + ChatEntry.TABLE_NAME;

    ChatDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
}

