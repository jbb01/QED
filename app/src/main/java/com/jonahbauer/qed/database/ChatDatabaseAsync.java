package com.jonahbauer.qed.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import com.jonahbauer.qed.chat.Message;

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
    private Mode mode;
    private ChatDatabaseReceiver receiver;

    private SQLiteDatabase sqLiteDatabase;

    private String query;
    private String[] queryArgs;

    private List<Message> insertAll;

    ChatDatabaseAsync(SQLiteDatabase sqLiteDatabase, ChatDatabaseReceiver receiver, String query, String[] queryArgs) {
        this.mode = Mode.QUERY;
        this.sqLiteDatabase = sqLiteDatabase;
        this.receiver = receiver;
        this.query = query;
        this.queryArgs = queryArgs;
    }

    ChatDatabaseAsync(SQLiteDatabase sqLiteDatabase, ChatDatabaseReceiver receiver, List<Message> messages) {
        this.mode = Mode.INSERT_ALL;
        this.sqLiteDatabase = sqLiteDatabase;
        this.receiver = receiver;
        this.insertAll = messages;
    }

    @Override
    protected Boolean doInBackground(Object... objects) {
        switch (mode) {
            case QUERY:
                query(receiver, query, queryArgs);
                break;
            case INSERT_ALL:
                insertAll(insertAll);
                break;
        }
        return true;
    }

    private void query(ChatDatabaseReceiver receiver, String query, String[] args) {
        Cursor cursor = sqLiteDatabase.rawQuery(query, args);

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

        cursor.close();

        receiver.onReceiveResult(messages);
    }

    private void insertAll(List<Message> messages) {
        sqLiteDatabase.beginTransaction();

        AtomicInteger i = new AtomicInteger();
        int j = messages.size();

        for (Message message : messages) {
            ContentValues value = new ContentValues();
            value.put(COLUMN_NAME_ID, message.id);
            value.put(COLUMN_NAME_USERID, message.userId);
            value.put(COLUMN_NAME_USERNAME, message.userName);
            value.put(COLUMN_NAME_BOTTAG, message.bottag);
            value.put(COLUMN_NAME_COLOR, message.color);
            value.put(COLUMN_NAME_MESSAGE, message.message);
            value.put(COLUMN_NAME_DATE, message.date);
            value.put(COLUMN_NAME_NAME, message.name);
            value.put(COLUMN_NAME_CHANNEL, message.channel);

            try {
                sqLiteDatabase.insertOrThrow(TABLE_NAME, null, value);
            } catch (SQLiteConstraintException ignored) {}

            publishProgress(i.incrementAndGet(), j);
        }

        sqLiteDatabase.setTransactionSuccessful();
        sqLiteDatabase.endTransaction();
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (!success && (mode == Mode.QUERY) && (receiver != null)) receiver.onDatabaseError();
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (mode == Mode.INSERT_ALL && (receiver != null)) receiver.onInsertAllUpdate(values[0], values[1]);
    }

    enum Mode {
        QUERY, INSERT_ALL
    }
}
