package com.jonahbauer.qed.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import com.jonahbauer.qed.chat.Message;

import java.util.ArrayList;
import java.util.List;

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
    private static int insertsRunning = 0;
    private ChatDatabaseHelper databaseHelper;
    private Mode mode;
    private ChatDatabaseReceiver receiver;

    @Override
    protected Boolean doInBackground(Object... objects) {
        if (objects.length < 2) return false;
        else {
            if (objects[0] instanceof Mode) mode = (Mode) objects[0];
            else return false;

            if (objects[1] instanceof ChatDatabaseHelper) databaseHelper = (ChatDatabaseHelper) objects[1];
            else return false;
        }

        switch (mode) {
            case QUERY:
                if (objects.length < 4) return false;
                else {
                    String query;
                    String[] args;

                    if (objects[2] instanceof ChatDatabaseReceiver) receiver = (ChatDatabaseReceiver) objects[2];
                    else return false;

                    if (objects[3] instanceof String) query = (String) objects[3];
                    else return false;

                    if (objects.length >= 5) {
                        if (objects[4] != null) {
                            if (objects[4] instanceof String[]) args = (String[]) objects[4];
                            else args = null;
                        } else args = null;
                    } else args = null;

                    query(receiver, query, args);
                    return true;
                }
            case INSERTALL:
                if (objects.length < 3) return false;
                else {
                    List messages;

                    if (objects[2] instanceof List) messages = (List) objects[2];
                    else return false;

                    if (objects.length >= 4) if (objects[3] instanceof ChatDatabaseReceiver) receiver = (ChatDatabaseReceiver) objects[3];

                    //noinspection unchecked
                    insertAll((List<Message>) messages);
                    return true;
                }
        }
        return true;
    }

    private void query(ChatDatabaseReceiver receiver, String query, String[] args) {
        SQLiteDatabase database = databaseHelper.getReadableDatabase();

        Cursor cursor = database.rawQuery(query, args);

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
        database.close();

        receiver.onReceiveResult(messages);
    }

    private void insertAll(List<Message> messages) {
        insertsRunning ++;
        SQLiteDatabase chatLogWritable = databaseHelper.getWritableDatabase();

        List<Message> doneMessages = new ArrayList<>();

        messages.stream().parallel().forEach(message -> {
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
                chatLogWritable.insertOrThrow(TABLE_NAME, null, value);
            } catch (SQLiteConstraintException ignored) {}

            doneMessages.add(message);

            publishProgress(doneMessages.size(), messages.size());
        });

        insertsRunning --;
        if (insertsRunning == 0) chatLogWritable.close();
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (!success && (mode == Mode.QUERY) && (receiver != null)) receiver.onDatabaseError();
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (mode == Mode.INSERTALL && (receiver != null)) receiver.onInsertAllUpdate(values[0], values[1]);
    }

    enum Mode {
        QUERY, INSERTALL
    }
}
