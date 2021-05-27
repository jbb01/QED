package com.jonahbauer.qed.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import com.jonahbauer.qed.model.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.jonahbauer.qed.database.ChatDatabaseContract.ChatEntry.COLUMN_NAME_BOTTAG;
import static com.jonahbauer.qed.database.ChatDatabaseContract.ChatEntry.COLUMN_NAME_CHANNEL;
import static com.jonahbauer.qed.database.ChatDatabaseContract.ChatEntry.COLUMN_NAME_COLOR;
import static com.jonahbauer.qed.database.ChatDatabaseContract.ChatEntry.COLUMN_NAME_DATE;
import static com.jonahbauer.qed.database.ChatDatabaseContract.ChatEntry.COLUMN_NAME_ID;
import static com.jonahbauer.qed.database.ChatDatabaseContract.ChatEntry.COLUMN_NAME_MESSAGE;
import static com.jonahbauer.qed.database.ChatDatabaseContract.ChatEntry.COLUMN_NAME_NAME;
import static com.jonahbauer.qed.database.ChatDatabaseContract.ChatEntry.COLUMN_NAME_USERID;
import static com.jonahbauer.qed.database.ChatDatabaseContract.ChatEntry.COLUMN_NAME_USERNAME;
import static com.jonahbauer.qed.database.ChatDatabaseContract.ChatEntry.TABLE_NAME;

public class ChatDatabaseAsync extends AsyncTask<Object, Integer, Boolean> {
    private final Mode mMode;
    private final ChatDatabaseReceiver mReceiver;
    private final ChatDatabaseHelper mDatabaseHelper;

    private String mQuery;
    private String[] mQueryArgs;

    private List<Message> mInsertAll;

    ChatDatabaseAsync(ChatDatabaseHelper databaseHelper, ChatDatabaseReceiver receiver, String query, String[] queryArgs) {
        this.mMode = Mode.QUERY;
        this.mDatabaseHelper = databaseHelper;
        this.mReceiver = receiver;
        this.mQuery = query;
        this.mQueryArgs = queryArgs;
    }

    ChatDatabaseAsync(ChatDatabaseHelper databaseHelper, ChatDatabaseReceiver receiver, List<Message> messages) {
        this.mMode = Mode.INSERT_ALL;
        this.mDatabaseHelper = databaseHelper;
        this.mReceiver = receiver;
        this.mInsertAll = messages;
    }

    @Override
    protected Boolean doInBackground(Object... objects) {
        switch (mMode) {
            case QUERY:
                query(mReceiver, mQuery, mQueryArgs);
                break;
            case INSERT_ALL:
                insertAll(mInsertAll);
                break;
        }
        return true;
    }

    private void query(ChatDatabaseReceiver receiver, String query, String[] args) {
        try (SQLiteDatabase chatReadable = mDatabaseHelper.getReadableDatabase()) {
            try (Cursor cursor = chatReadable.rawQuery(query, args)) {
                List<Message> messages = new ArrayList<>();

                while (cursor.moveToNext()) {
                    messages.add(new Message(
                            cursor.getString(cursor.getColumnIndex(COLUMN_NAME_NAME)),
                            cursor.getString(cursor.getColumnIndex(COLUMN_NAME_MESSAGE)),
                            cursor.getString(cursor.getColumnIndex(COLUMN_NAME_DATE)),
                            cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_USERID)),
                            cursor.getString(cursor.getColumnIndex(COLUMN_NAME_USERNAME)),
                            cursor.getString(cursor.getColumnIndex(COLUMN_NAME_COLOR)),
                            cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_ID)),
                            cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_BOTTAG)),
                            cursor.getString(cursor.getColumnIndex(COLUMN_NAME_CHANNEL))
                    ));
                }

                receiver.onReceiveResult(messages);
            }
        }
    }

    private void insertAll(List<Message> messages) {
        try (SQLiteDatabase chatWriteable = mDatabaseHelper.getWritableDatabase()) {
            chatWriteable.beginTransaction();
            try {

                AtomicInteger i = new AtomicInteger();
                int j = messages.size();

                for (Message message : messages) {
                    ContentValues value = new ContentValues();
                    value.put(COLUMN_NAME_ID, message.getId());
                    value.put(COLUMN_NAME_USERID, message.getUserId());
                    value.put(COLUMN_NAME_USERNAME, message.getUserName());
                    value.put(COLUMN_NAME_BOTTAG, message.getBottag());
                    value.put(COLUMN_NAME_COLOR, message.getColor());
                    value.put(COLUMN_NAME_MESSAGE, message.getMessage());
                    value.put(COLUMN_NAME_DATE, message.getDate());
                    value.put(COLUMN_NAME_NAME, message.getName());
                    value.put(COLUMN_NAME_CHANNEL, message.getChannel());

                    try {
                        chatWriteable.insertOrThrow(TABLE_NAME, null, value);
                    } catch (SQLiteConstraintException ignored) {}

                    publishProgress(i.incrementAndGet(), j);
                }

                chatWriteable.setTransactionSuccessful();
            } finally {
                chatWriteable.endTransaction();
            }
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (!success && (mMode == Mode.QUERY) && (mReceiver != null)) mReceiver.onDatabaseError();
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (mMode == Mode.INSERT_ALL && (mReceiver != null)) mReceiver.onInsertAllUpdate(values[0], values[1]);
    }

    enum Mode {
        QUERY, INSERT_ALL
    }
}
