package com.jonahbauer.qed.networking;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.mainFragments.LogFragment;
import com.jonahbauer.qed.model.Message;
import com.jonahbauer.qed.model.adapter.MessageAdapter;
import com.jonahbauer.qed.networking.async.QEDPageStreamReceiver;
import com.jonahbauer.qed.networking.downloadManager.Download;
import com.jonahbauer.qed.networking.downloadManager.DownloadListener;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.experimental.UtilityClass;

@UtilityClass
public class QEDChatPages {
    private static final Map<String, ParseThread> threads = new HashMap<>();
    private static final Map<String, Download> asyncTasks = new HashMap<>();

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {STATUS_DOWNLOAD_PENDING, STATUS_DOWNLOAD_RUNNING, STATUS_DOWNLOAD_FINISHED, STATUS_PARSE_THREAD_ALIVE, STATUS_PARSE_THREAD_TERMINATED}, flag = true)
    public @interface Status {}
    public static final int STATUS_DOWNLOAD_PENDING = 1;
    public static final int STATUS_DOWNLOAD_RUNNING = 1 << 1;
    public static final int STATUS_DOWNLOAD_FINISHED = 1 << 2;
    public static final int STATUS_PARSE_THREAD_ALIVE = 1 << 4;
    public static final int STATUS_PARSE_THREAD_TERMINATED = 1 << 5;

    /**
     * This method asynchronously get the qed chat log and parses it to {@code Message} objects
     * @param tag a tag used when calling receiver methods
     * @param options a string specifying which log to get
     * @param chatLogReceiver a receiver handling progress updates and errors
     */
    @Nullable
    public static Download getChatLog(String tag, @NonNull LogFragment.LogRequest logRequest, LogDownloadListener listener) {
        boolean isFile = logRequest instanceof LogFragment.FileLogRequest;

        if (isFile) {
            Uri fileUri = ((LogFragment.FileLogRequest) logRequest).getFile();
            listener.onDownloadCompleted(fileUri);
            return null;
        }

        Context context = listener.mContext;

        File file = new File(context.getExternalCacheDir(), "chatLog.log");
        file.deleteOnExit();


        Uri uri = Uri.parse(NetworkConstants.CHAT_SERVER_HISTORY + logRequest.getQueryString());
        String title = context.getString(R.string.download_notification_title);
        String description = context.getString(R.string.download_notification_log_description);
        Download download = new Download(Feature.CHAT, uri, file, null, listener, title, description);

        asyncTasks.put(tag, download);

        Download.startDownload(download);

        return download;
    }


    @Status
    public static int getStatus(String tag) {
        Download download = asyncTasks.get(tag);
        ParseThread parse = threads.get(tag);

        int out = 0;

        if (download != null) {
            int downloadStatus = download.getDownloadStatus();
            switch (downloadStatus) {
                case DownloadManager.STATUS_PENDING:
                    out |= STATUS_DOWNLOAD_PENDING;
                    break;
                case DownloadManager.STATUS_RUNNING:
                case DownloadManager.STATUS_PAUSED:
                    out |= STATUS_DOWNLOAD_RUNNING;
                    break;
                case DownloadManager.STATUS_SUCCESSFUL:
                case DownloadManager.STATUS_FAILED:
                case -1:
                    out |= STATUS_DOWNLOAD_FINISHED;
                    break;
            }
        }

        if (parse != null) {
            if (parse.getState() == Thread.State.TERMINATED) {
                out |= STATUS_PARSE_THREAD_TERMINATED;
            } else {
                out |= STATUS_PARSE_THREAD_ALIVE;
            }
        }

        return out;
    }

    /**
     * Cancels the ui async task, the download manager and the parse thread
     */
    public static void interrupt(String tag) {
        Download download = asyncTasks.get(tag);
        ParseThread parse = threads.get(tag);

        if (download != null) {
            DownloadListener downloadListener = download.getListener();
            if (downloadListener != null) download.detachListener(downloadListener);

            Download.stopDownload(download);
        }
        if (parse != null) parse.cancel();
    }


    private static class ParseThread extends Thread {
        private static final Handler handler = new Handler(Looper.getMainLooper());

        private final String mTag;
        private final long mId;
        private final MessageAdapter mOut;
        private final Uri mSourceFileUri;
        private final Application mContext;
        private final QEDPageStreamReceiver<String> mChatLogReceiver;
        private final DownloadListener mDownloadListener;

        private boolean mCanceled;


        private ParseThread(String tag, long id, @NonNull MessageAdapter out, Uri sourceFileUri, Application context, QEDPageStreamReceiver<String> chatLogReceiver, DownloadListener downloadListener) {
            this.mTag = tag;
            this.mId = id;
            this.mOut = out;
            this.mSourceFileUri = sourceFileUri;
            this.mContext = context;
            this.mChatLogReceiver = chatLogReceiver;
            this.mDownloadListener = downloadListener;
        }

        @Override
        public void run() {
            List<Closeable> closeables = new LinkedList<>();

            // get reader from uri or tmp file
            Reader inReader;
            Reader inReader2;
            long size = -1;
            try {
                // Two streams required: one for getting line count, one for reading data
                InputStream inputStream = mContext.getContentResolver().openInputStream(mSourceFileUri);
                InputStream inputStream2 = mContext.getContentResolver().openInputStream(mSourceFileUri);
                closeables.add(inputStream);
                closeables.add(inputStream2);

                ParcelFileDescriptor parcelFileDescriptor = mContext.getContentResolver().openFileDescriptor(mSourceFileUri, "r");
                if (parcelFileDescriptor != null) {
                    size = parcelFileDescriptor.getStatSize();
                    parcelFileDescriptor.close();
                }

                if (inputStream == null || inputStream2 == null)
                    throw new FileNotFoundException("InputStream is null");

                inReader = new InputStreamReader(inputStream);
                inReader2 = new InputStreamReader(inputStream2);
                closeables.add(inReader);
                closeables.add(inReader2);
            } catch (IOException e) {
                Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
                mDownloadListener.onError(mId, null, e);
                return;
            }

            if (size != -1)
                mDownloadListener.onProgressUpdate(mId, DownloadManager.STATUS_SUCCESSFUL, (int) size);


            LinkedList<Message> tmpMessageList = new LinkedList<>();
            try (BufferedReader reader = new BufferedReader(inReader); BufferedReader reader2 = new BufferedReader(inReader2)) {
                // get BufferedReader and lineCount
                final long lineCount = reader.lines().count() - 2;
                mDownloadListener.onProgressUpdate(mId, 0, (int) lineCount);
                reader.close();


                AtomicInteger index = new AtomicInteger(0);
                Iterator<String> lines = reader2.lines().sequential().iterator();

                while (lines.hasNext()) {
                    if (mCanceled) return;

                    String string = lines.next();
                    Message msg = Message.interpretJSONMessage(string);

                    if (msg != null) {
                        tmpMessageList.add(msg);
                    }

                    final int j = index.getAndIncrement();
                    if (j % 947 == 0) { // make it look random
                        if (Thread.currentThread().isInterrupted()) {
                            return;
                        }

                        mDownloadListener.onProgressUpdate(mId, -j, (int) lineCount);
                    }
                }

                handler.post(() -> mOut.addAll(tmpMessageList));


                if (mChatLogReceiver != null)
                    handler.post(() -> mChatLogReceiver.onPageReceived(mTag));
            } catch (IOException e) {
                Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
                mDownloadListener.onError(mId, null, e);
            } finally {
                // close all closeables
                for (Closeable closeable : closeables) {
                    if (closeable != null) try { closeable.close(); } catch (IOException ignored) {}
                }
            }
        }

        public void cancel() {
            mCanceled = true;
        }
    }

    public static class LogDownloadListener extends DownloadListener {
        private final Handler handler = new Handler(Looper.getMainLooper());

        private final String mTag;
        private final MessageAdapter mOut;
        private final Application mContext;
        private final QEDPageStreamReceiver<String> mChatLogReceiver;

        public LogDownloadListener(String tag, MessageAdapter out, Application context, QEDPageStreamReceiver<String> chatLogReceiver) {
            this.mTag = tag;
            this.mOut = out;
            this.mContext = context;
            this.mChatLogReceiver = chatLogReceiver;
        }

        private void onDownloadCompleted(Uri file) {
            ParseThread thread = new ParseThread(mTag, -1, mOut, file, mContext, mChatLogReceiver, this);
            thread.start();

            threads.put(mTag, thread);
        }

        @Override
        public void onDownloadCompleted(long id, Cursor download) {
            // asynchronously parse json messages
            ParseThread thread = new ParseThread(mTag, id, mOut, mDownloadManager.getUriForDownloadedFile(id), mContext, mChatLogReceiver, this);
            thread.start();

            threads.put(mTag, thread);
        }

        @Override
        public void onError(long id, @Nullable String reason, @Nullable Throwable cause) {
            super.onError(id, reason, cause);
            if (mChatLogReceiver != null)
                handler.post(() -> mChatLogReceiver.onError(mTag, Reason.NETWORK, null));
        }

        @Override
        public void onProgressUpdate(long id, int status, int byteSoFar) {
            if (mChatLogReceiver != null) {
                handler.post(() -> mChatLogReceiver.onProgressUpdate(mTag, -status, byteSoFar));
            }
        }

        @Override
        public void onAttach(long id) {}

        @Override
        public void onDetach(long id) {}
    }
}
