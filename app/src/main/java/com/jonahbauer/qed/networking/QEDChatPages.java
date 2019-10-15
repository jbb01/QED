package com.jonahbauer.qed.networking;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.chat.Message;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings({"ResultOfMethodCallIgnored"})
public abstract class QEDChatPages {
    public static AsyncTask getChatLog(String tag, String options, List<Message> out, QEDPageStreamReceiver chatLogReceiver) {
        Application application = Application.getContext();

        File file;
        try {
            file = File.createTempFile("chatLog", ".log", application.getExternalCacheDir());
        } catch (IOException e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
            chatLogReceiver.onStreamError(tag);
            return null;
        }

        QEDPageStreamReceiver receiver = new QEDPageStreamReceiver() {
            @Override
            public void onPageReceived(String tag) {
                chatLogReceiver.onProgressUpdate(tag, Integer.MIN_VALUE, Integer.MIN_VALUE);
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {

                    // get BufferedReader and lineCount
                    long lineCount;
                    lineCount = reader.lines().count() - 2;
                    chatLogReceiver.onProgressUpdate(tag, 0, lineCount - 2);

                    // asynchronously parse json messages
                    Handler handler = new Handler(Looper.getMainLooper());
                    AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
                        try (Scanner scanner = new Scanner(file)) {
                            AtomicInteger i = new AtomicInteger(0);

                            // skip first line
                            scanner.nextLine();

                            while (scanner.hasNextLine()) {
                                String str = scanner.nextLine();
                                Message msg = Message.interpretJSONMessage(str, false);

                                if (msg != null)
                                    out.add(msg);

                                int j = i.getAndIncrement();
                                if (j % 1000 == 0)
                                    handler.post(() -> chatLogReceiver.onProgressUpdate(tag, j, lineCount));
                            }

                            scanner.close();

                            handler.post(() -> chatLogReceiver.onPageReceived(tag));
                            if (file != null && file.exists()) file.delete();
                        } catch (FileNotFoundException e) {
                            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
                            onStreamError(tag);
                        }
                    });
                } catch (IOException e) {
                    Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
                    onStreamError(tag);
                }
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
            if (file != null && file.exists()) file.delete();
            return null;
        }

        log.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);

        return log;
    }
}
