package com.jonahbauer.qed.networking;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.chat.Message;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings({"ResultOfMethodCallIgnored"})
public abstract class QEDChatPages {
    private static final Map<String, Thread> threads = new HashMap<>();
    private static final Map<String, AsyncTask> asyncTasks = new HashMap<>();

    /**
     * This method asynchronously get the qed chat log and parses it to {@code Message} objects
     * @param tag a tag used when calling receiver methods
     * @param options a string specifying which log to get
     * @param out a list which will contain the chat log after execution is done
     * @param chatLogReceiver a receiver handling progress updates and errors
     * @return the AsyncTask getting the chat log. this is not the same task as the one which parses the string log to a list of message objects
     */
    @Nullable
    public static AsyncTask getChatLog(String tag, @NonNull String options, final List<Message> out, QEDPageStreamReceiver chatLogReceiver) {
        boolean isFile = options.startsWith("FILE");

        Application application = Application.getContext();

        File file;
        try {
            if (!isFile) {
                file = File.createTempFile("chatLog", ".log", application.getExternalCacheDir());
                file.deleteOnExit();
            } else
                file = null;
        } catch (IOException e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
            chatLogReceiver.onStreamError(tag);
            return null;
        }

        QEDPageStreamReceiver receiver = new QEDPageStreamReceiver() {
            @Override
            public void onPageReceived(String tag) {
                chatLogReceiver.onProgressUpdate(tag, Integer.MIN_VALUE, Integer.MIN_VALUE);

                // asynchronously parse json messages
                Runnable runnable = () -> {
                    final List<Message> messageList;
                    if (out != null) messageList = out;
                    else messageList = new ArrayList<>(1000);

                    List<Closeable> closeables = new LinkedList<>();

                    // get reader from uri or tmp file
                    Reader inReader;
                    Reader inReader2;
                    try {
                        if (isFile) {
                            InputStream inputStream = application.getContentResolver().openInputStream(Uri.parse(options.substring(4)));
                            InputStream inputStream2 = application.getContentResolver().openInputStream(Uri.parse(options.substring(4)));
                            closeables.add(inputStream);
                            closeables.add(inputStream2);

                            if (inputStream == null || inputStream2 == null)
                                throw new FileNotFoundException("InputStream is null");

                            inReader = new InputStreamReader(inputStream);
                            inReader2 = new InputStreamReader(inputStream2);
                            closeables.add(inReader);
                            closeables.add(inReader2);
                        } else {
                            inReader = new FileReader(file);
                            inReader2 = new FileReader(file);
                            closeables.add(inReader);
                            closeables.add(inReader2);
                        }
                    } catch (FileNotFoundException e) {
                        Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
                        onStreamError(tag);
                        return;
                    }


                    try (BufferedReader reader = new BufferedReader(inReader); BufferedReader reader2 = new BufferedReader(inReader2)) {
                        // get BufferedReader and lineCount
                        final long lineCount = reader.lines().count() - 2;
                        chatLogReceiver.onProgressUpdate(tag, 0, lineCount - 2);
                        reader.close();

                        Handler handler = new Handler(Looper.getMainLooper());

                        AtomicInteger index = new AtomicInteger(0);
                        reader2.lines().sequential().forEachOrdered(string -> {
                            Message msg = Message.interpretJSONMessage(string);

                            if (msg != null) {
                                messageList.add(msg);
                            }

                            final int j = index.getAndIncrement();
                            if (j % 947 == 0) { // make it look random
                                if (Thread.currentThread().isInterrupted()) {
                                    if (file != null && file.exists()) file.delete();
                                    return;
                                }
                                handler.post(() -> chatLogReceiver.onProgressUpdate(tag, j, lineCount));
                            }
                        });


                        handler.post(() -> chatLogReceiver.onPageReceived(tag));
                        if (file != null && file.exists()) file.delete();
                    } catch (IOException e) {
                        Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
                        onStreamError(tag);
                    } finally {
                        // close all closeables
                        for (Closeable closeable : closeables) {
                            if (closeable != null) try { closeable.close(); } catch (IOException ignored) {}
                        }
                        if (file != null && file.exists()) file.delete();
                    }
                };

                Thread thread = new Thread(runnable);
                thread.start();

                threads.put(tag, thread);
            }

            @Override
            public void onNetworkError(String tag) {
                if (file != null && file.exists()) file.delete();
                if (chatLogReceiver != null) chatLogReceiver.onNetworkError(tag);
            }

            @Override
            public void onStreamError(String tag) {
                if (file != null && file.exists()) file.delete();
                if (chatLogReceiver != null) chatLogReceiver.onStreamError(tag);
            }

            @Override
            public void onProgressUpdate(String tag, long done, long total) {
                if (total == -1) chatLogReceiver.onProgressUpdate(tag, done, Integer.MIN_VALUE);
            }
        };

        if (isFile) {
            receiver.onPageReceived(tag);
            return null;
        }

        AsyncLoadQEDPageToStream log;
        try {
            log = new AsyncLoadQEDPageToStream(
                    AsyncLoadQEDPageToStream.Feature.CHAT,
                    application.getString(R.string.chat_server_history) + "?" + options,
                    receiver,
                    tag,
                    new FileOutputStream(file)
            );
        } catch (FileNotFoundException e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
            chatLogReceiver.onStreamError(tag);
            if (file.exists()) file.delete();
            return null;
        }

        log.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);

        return log;
    }

    /**
     * interrupts everything async started from this class
     */
    public static void interruptAll() {
        for (String key : threads.keySet()) {
            Thread thread = threads.get(key);
            if (thread != null) thread.interrupt();
        }
        for (String key : asyncTasks.keySet()) {
            AsyncTask asyncTask = asyncTasks.get(key);
            if (asyncTask != null) asyncTask.cancel(true);
        }

        threads.clear();
        asyncTasks.clear();
    }
}
