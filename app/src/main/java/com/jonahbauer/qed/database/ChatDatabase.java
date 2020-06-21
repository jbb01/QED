package com.jonahbauer.qed.database;

import android.content.ContentValues;
import android.content.Context;
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
    private ChatDatabaseHelper mChatDatabaseHelper;
    private ChatDatabaseReceiver mReceiver;
    private List<ChatDatabaseAsync> mAsyncTasks;

    public void init(Context context, ChatDatabaseReceiver receiver) {
        mChatDatabaseHelper = new ChatDatabaseHelper(context);
        mAsyncTasks = new ArrayList<>();
        this.mReceiver = receiver;
    }

    public void close() {
        mChatDatabaseHelper.getWritableDatabase().close();
        mChatDatabaseHelper.getReadableDatabase().close();

        if (mChatDatabaseHelper != null) mChatDatabaseHelper.close();
        mAsyncTasks.forEach(async -> {
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

        try (SQLiteDatabase chatLogWritable = mChatDatabaseHelper.getWritableDatabase()) {
            try {
                row = chatLogWritable.insertOrThrow(TABLE_NAME, null, value);
            } catch (SQLiteConstraintException ignored) {}
        }

        return row;
    }

    public void insertAll(List<Message> messages) {
        ChatDatabaseAsync async = new ChatDatabaseAsync(mChatDatabaseHelper, mReceiver, messages);
        mAsyncTasks.add(async);
        async.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void query(String sql, String[] selectionArgs) {
        ChatDatabaseAsync async = new ChatDatabaseAsync(mChatDatabaseHelper, mReceiver, sql, selectionArgs);
        mAsyncTasks.add(async);
        async.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void clear() {
        try (SQLiteDatabase writableDatabase = mChatDatabaseHelper.getWritableDatabase()) {
            mChatDatabaseHelper.clear(writableDatabase);
        }
    }
}

