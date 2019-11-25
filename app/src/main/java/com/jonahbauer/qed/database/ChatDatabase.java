package com.jonahbauer.qed.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

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

public class ChatDatabase {
    private ChatDatabaseHelper chatDatabaseHelper;
    private long lastId = Long.MIN_VALUE;
    private ChatDatabaseReceiver receiver;
    private List<ChatDatabaseAsync> asyncTasks;

    public void init(Context context, ChatDatabaseReceiver receiver) {
        chatDatabaseHelper = new ChatDatabaseHelper(context);
        asyncTasks = new ArrayList<>();
        this.receiver = receiver;
    }

    public void close() {
        chatDatabaseHelper.getWritableDatabase().close();
        chatDatabaseHelper.getReadableDatabase().close();

        if (chatDatabaseHelper != null) chatDatabaseHelper.close();
        asyncTasks.forEach(async -> {
            if (!async.isCancelled()) async.cancel(true);
        });
    }

    @SuppressWarnings("UnusedReturnValue")
    public long insert(@NonNull Message message) {
        ContentValues value = new ContentValues();
        value.put(COLUMN_NAME_ID,message.id);
        value.put(COLUMN_NAME_USERID,message.userId);
        value.put(COLUMN_NAME_USERNAME,message.userName);
        value.put(COLUMN_NAME_BOTTAG,message.bottag);
        value.put(COLUMN_NAME_COLOR,message.color);
        value.put(COLUMN_NAME_MESSAGE,message.message);
        value.put(COLUMN_NAME_DATE,message.date);
        value.put(COLUMN_NAME_NAME,message.name);
        value.put(COLUMN_NAME_CHANNEL,message.channel);

        long row = -1;

        SQLiteDatabase chatLogWritable = chatDatabaseHelper.getWritableDatabase();

        try {
            row = chatLogWritable.insertOrThrow(TABLE_NAME, null, value);
        } catch (SQLiteConstraintException ignored) {}

        if (lastId > 0 && message.id > lastId) lastId = message.id;

        return row;
    }

    public void insertAll(List<Message> messages) {
        ChatDatabaseAsync async = new ChatDatabaseAsync(chatDatabaseHelper.getWritableDatabase(), receiver, messages);
        asyncTasks.add(async);
        async.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public long getLastId() {
        if (lastId > 0) return lastId;
        else {
            SQLiteDatabase chatLogReadable = chatDatabaseHelper.getReadableDatabase();

            Cursor cursor = chatLogReadable.query(
                    TABLE_NAME,
                    new String[]{COLUMN_NAME_ID},
                    null,
                    null,
                    null,
                    null,
                    COLUMN_NAME_ID + " DESC",
                    "1"
            );


            if (cursor.moveToFirst()) {
                lastId = cursor.getLong(cursor.getColumnIndex(COLUMN_NAME_ID));
            } else {
                lastId = Integer.MAX_VALUE;
            }

            cursor.close();
            chatLogReadable.close();

            return lastId;
        }
    }

    public void query(String sql, String[] selectionArgs) {
        ChatDatabaseAsync async = new ChatDatabaseAsync(chatDatabaseHelper.getReadableDatabase(), receiver, sql, selectionArgs);
        asyncTasks.add(async);
        async.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void clear() {
        SQLiteDatabase writableDatabase = chatDatabaseHelper.getWritableDatabase();
        writableDatabase.beginTransaction();
        chatDatabaseHelper.clear(writableDatabase);
        writableDatabase.setTransactionSuccessful();
        writableDatabase.endTransaction();
    }
}

