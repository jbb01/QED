package com.jonahbauer.qed.networking;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.chat.Message;
import com.jonahbauer.qed.chat.MessageAdapter;
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
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class QEDChatPages {
    private static final Map<String, ParseThread> threads = new HashMap<>();
    private static final Map<String, Download> asyncTasks = new HashMap<>();

    /**
     * This method asynchronously get the qed chat log and parses it to {@code Message} objects
     * @param tag a tag used when calling receiver methods
     * @param options a string specifying which log to get
     * @param chatLogReceiver a receiver handling progress updates and errors
     */
    @Nullable
    public static Download getChatLog(String tag, @NonNull String options, QEDPageStreamReceiver chatLogReceiver, LogDownloadListener listener) {
        boolean isFile = options.startsWith("FILE");

        SoftReference<Application> applicationReference = Application.getApplicationReference();
        Application application = applicationReference.get();

        if (application == null) {
            chatLogReceiver.onError(tag, null, new AssertionError("Application is null!"));
            return null;
        }

        Uri fileUri;
        File file;
        if (isFile) {
            file = null;
            fileUri = Uri.parse(options.substring(4));
        } else {
            file = new File(application.getExternalCacheDir(), "chatLog.log");
            file.deleteOnExit();
            fileUri = Uri.fromFile(file);
        }

        if (isFile) {
            listener.onDownloadCompleted(fileUri);
            return null;
        }

        Uri uri = Uri.parse(application.getString(R.string.chat_server_history) + "?" + options);
        Download download = new Download(Download.Feature.CHAT, uri, file, null, listener, application.getString(R.string.download_notification_title), application.getString(R.string.download_notification_log_description));

        asyncTasks.put(tag, download);

        Download.startDownload(download);

        return download;
    }


    /**
     * @return  1 -> none of the below
     *          2 -> download pending
     *          4 -> download running
     *          8 -> download finished
     *          16 -> parsing thread alive
     *          32 -> parsing thread terminated
     */
    public static int getStatus(String tag) {
        Download download = asyncTasks.get(tag);
        ParseThread parse = threads.get(tag);

        int out = 0;

        if (download != null) {
            int downloadStatus = download.getDownloadStatus();
            switch (downloadStatus) {
                case DownloadManager.STATUS_PENDING:
                    out |= 1 << 1;
                    break;
                case DownloadManager.STATUS_RUNNING:
                case DownloadManager.STATUS_PAUSED:
                    out |= 1 << 2;
                    break;
                case DownloadManager.STATUS_SUCCESSFUL:
                case DownloadManager.STATUS_FAILED:
                case -1:
                    out |= 1 << 3;
                    break;
            }
        }

        if (parse != null) {
            if (parse.getState() == Thread.State.TERMINATED) {
                out |= 1 << 5;
            } else {
                out |= 1 << 4;
            }
        }

        return out == 0 ? 1 : out;
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

        private final String tag;
        private final long id;
        private final MessageAdapter out;
        private final Uri sourceFileUri;
        private final Context context;
        private final QEDPageStreamReceiver chatLogReceiver;
        private final DownloadListener downloadListener;

        private boolean canceled;


        private ParseThread(String tag, long id, @NonNull MessageAdapter out, Uri sourceFileUri, Context context, QEDPageStreamReceiver chatLogReceiver, DownloadListener downloadListener) {
            this.tag = tag;
            this.id = id;
            this.out = out;
            this.sourceFileUri = sourceFileUri;
            this.context = context;
            this.chatLogReceiver = chatLogReceiver;
            this.downloadListener = downloadListener;
        }

        @Override
        public void run() {
            List<Closeable> closeables = new LinkedList<>();

            // get reader from uri or tmp file
            Reader inReader;
            Reader inReader2;
            long size = -1;
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(sourceFileUri);
                InputStream inputStream2 = context.getContentResolver().openInputStream(sourceFileUri);
                closeables.add(inputStream);
                closeables.add(inputStream2);

                ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(sourceFileUri, "r");
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
                downloadListener.onError(id, null, e);
                return;
            }

            if (size != -1)
                downloadListener.onProgressUpdate(id, DownloadManager.STATUS_SUCCESSFUL, (int) size);


            LinkedList<Message> tmpMessageList = new LinkedList<>();
            try (BufferedReader reader = new BufferedReader(inReader); BufferedReader reader2 = new BufferedReader(inReader2)) {
                // get BufferedReader and lineCount
                final long lineCount = reader.lines().count() - 2;
                downloadListener.onProgressUpdate(id, 0, (int) lineCount);
                reader.close();


                AtomicInteger index = new AtomicInteger(0);
                Iterator<String> lines = reader2.lines().sequential().iterator();

                while (lines.hasNext()) {
                    if (canceled) return;

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

                        downloadListener.onProgressUpdate(id, -j, (int) lineCount);
                    }
                }

                handler.post(() -> out.addAll(tmpMessageList));


                if (chatLogReceiver != null)
                    handler.post(() -> chatLogReceiver.onPageReceived(tag, null));
            } catch (IOException e) {
                Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
                downloadListener.onError(id, null, e);
            } finally {
                // close all closeables
                for (Closeable closeable : closeables) {
                    if (closeable != null) try { closeable.close(); } catch (IOException ignored) {}
                }
            }
        }

        public void cancel() {
            canceled = true;
        }
    }


    // TODO context leak
    public static class LogDownloadListener extends DownloadListener {
        private final Handler handler = new Handler(Looper.getMainLooper());

        private final String tag;
        private final MessageAdapter out;
        private final Context context;
        private final QEDPageStreamReceiver chatLogReceiver;

        public LogDownloadListener(String tag, MessageAdapter out, Context context, QEDPageStreamReceiver chatLogReceiver) {
            this.tag = tag;
            this.out = out;
            this.context = context;
            this.chatLogReceiver = chatLogReceiver;
        }

        private void onDownloadCompleted(Uri file) {
            ParseThread thread = new ParseThread(tag, -1, out, file, context, chatLogReceiver, this);
            thread.start();

            threads.put(tag, thread);
        }

        @Override
        public void onDownloadCompleted(long id, Cursor download) {
            // asynchronously parse json messages
            ParseThread thread = new ParseThread(tag, id, out, downloadManager.getUriForDownloadedFile(id), context, chatLogReceiver, this);
            thread.start();

            threads.put(tag, thread);
        }

        @Override
        public void onError(long id, @Nullable String reason, @Nullable Throwable cause) {
            super.onError(id, reason, cause);
            if (chatLogReceiver != null)
                handler.post(() -> chatLogReceiver.onError(tag, QEDPageReceiver.REASON_NETWORK, null));
        }

        @Override
        public void onProgressUpdate(long id, int status, int byteSoFar) {
            if (chatLogReceiver != null) {
                handler.post(() -> chatLogReceiver.onProgressUpdate(tag, -status, byteSoFar));
            }
        }

        @Override
        public void onAttach(long id) {}

        @Override
        public void onDetach(long id) {}
    }
}
