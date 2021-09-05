package com.jonahbauer.qed.networking;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jonahbauer.qed.model.Message;
import com.jonahbauer.qed.networking.async.AsyncLoadQEDPageToStream;
import com.jonahbauer.qed.networking.async.QEDPageStreamReceiver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import lombok.experimental.UtilityClass;

import static com.jonahbauer.qed.model.viewmodel.LogViewModel.FileLogRequest;
import static com.jonahbauer.qed.model.viewmodel.LogViewModel.LogRequest;

@UtilityClass
public class QEDChatPages {
    /**
     * This method asynchronously get the qed chat log and parses it to {@code Message} objects
     * @param logRequest a log request
     * @param listener a receiver handling progress updates and errors
     */
    @Nullable
    public static AsyncTask<?,?,?> getChatLog(Context context, @NonNull LogRequest logRequest, @NonNull Uri file, QEDPageStreamReceiver<Uri> listener) {
        if (logRequest instanceof FileLogRequest) throw new IllegalArgumentException();


        OutputStream fos;
        try {
            fos = context.getContentResolver().openOutputStream(file);
        } catch (IOException e) {
            listener.onError(file, Reason.guess(e), e);
            return null;
        }

        AsyncLoadQEDPageToStream<Uri> async = new AsyncLoadQEDPageToStream<>(
                Feature.CHAT,
                NetworkConstants.CHAT_SERVER_HISTORY + logRequest.getQueryString(),
                file,
                listener,
                fos
        );

        async.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        return async;
    }

    public static AsyncTask<?,?,?> parseChatLog(Application context, @NonNull Uri file, QEDPageStreamReceiver<List<Message>> listener) {
        ParseTask parseTask = new ParseTask(context, file, listener);

        parseTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        return parseTask;
    }

    private static class ParseTask extends AsyncTask<Void, Long, List<Message>> {
        private static final String LOG_TAG = ParseTask.class.getName();

        private final Application mContext;
        private final Uri mFile;
        private final QEDPageStreamReceiver<List<Message>> mReceiver;
        private Throwable mException;

        private ParseTask(Application context, Uri file, QEDPageStreamReceiver<List<Message>> listener) {
            this.mContext = context;
            this.mFile = file;
            this.mReceiver = listener;
        }

        @Override
        protected List<Message> doInBackground(Void... voids) {
            long lineCount;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(mContext.getContentResolver().openInputStream(mFile)))) {
                lineCount = reader.lines().count();
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                mException = e;
                return null;
            }

            if (lineCount > Integer.MAX_VALUE) {
                mException = new IndexOutOfBoundsException();
                return null;
            }

            List<Message> messages = new ArrayList<>((int) lineCount);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(mContext.getContentResolver().openInputStream(mFile)))) {
                long index = 0;
                long lastUpdate = System.currentTimeMillis();

                Iterator<String> lines = reader.lines().sequential().iterator();
                while (lines.hasNext()) {
                    if (isCancelled()) return null;

                    String string = lines.next();
                    Message msg = Message.interpretJSONMessage(string);

                    if (msg != null) {
                        messages.add(msg);
                    }

                    index++;

                    if (System.currentTimeMillis() - lastUpdate > 33) { // make it look random
                        lastUpdate = System.currentTimeMillis();
                        publishProgress(index, lineCount);
                    }
                }
                publishProgress(lineCount, lineCount);

                return messages;
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                mException = e;
                return null;
            }
        }

        @Override
        protected void onProgressUpdate(Long... values) {
            mReceiver.onProgressUpdate(Collections.emptyList(), values[0], values[1]);
        }

        @Override
        protected void onPostExecute(List<Message> messages) {
            if (messages != null) {
                mReceiver.onPageReceived(messages);
            } else if (mException != null) {
                mReceiver.onError(Collections.emptyList(), Reason.guess(mException), mException);
            }
        }
    }
}
