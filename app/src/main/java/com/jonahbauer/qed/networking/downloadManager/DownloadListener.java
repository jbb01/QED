package com.jonahbauer.qed.networking.downloadManager;

import android.app.DownloadManager;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jonahbauer.qed.Application;

public abstract class DownloadListener extends AsyncTask<Void, Long, Void> {
    protected final Handler mHandler = new Handler(Looper.getMainLooper());
    protected long mId;

    private boolean mAttached;
    protected DownloadManager mDownloadManager;

    public abstract void onDownloadCompleted(long id, Cursor download);
    @CallSuper
    public void onError(long id, @Nullable String reason, @Nullable Throwable cause) {
        Log.e(Application.LOG_TAG_ERROR, id + ": " + reason, cause);
    }
    public abstract void onProgressUpdate(long id, int status, int bytesSoFar);
    public abstract void onAttach(long id);
    public abstract void onDetach(long id);

    @SuppressWarnings("UnusedReturnValue")
    final boolean attach(long id, @SuppressWarnings("SameParameterValue") @NonNull DownloadManager downloadManager) {
        if (mAttached) {
            Log.e(Application.LOG_TAG_ERROR, null, new IllegalStateException("Cannot attach listener: the listener has already been attached (a download listener can be attached only once)"));
            return false;
        }

        onAttach(id);
        mAttached = true;

        this.mId = id;
        this.mDownloadManager = downloadManager;

        executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
        return true;
    }

    final void detach() {
        onDetach(mId);

        cancel(true);
    }

    @Nullable
    @Override
    protected final Void doInBackground(Void... voids) {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(mId);

        // query download status x seconds
        int downloadedSoFar = 0;
        while (!isCancelled()) {
            Cursor cursor = mDownloadManager.query(query);
            if (cursor.moveToFirst()) { // should always be true
                int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                int status = cursor.getInt(statusIndex);

                if (status == DownloadManager.STATUS_RUNNING) {
                    int byteIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                    downloadedSoFar = cursor.getInt(byteIndex);
                }

                if (status == DownloadManager.STATUS_FAILED) {
                    mHandler.post(() -> {
                        try {
                            int reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                            int reason = cursor.getInt(reasonIndex);

                            switch (reason) {
                                default:
                                case DownloadManager.ERROR_UNKNOWN:
                                    onError(mId, "ERROR_UNKNOWN", null);
                                    break;
                                case DownloadManager.ERROR_FILE_ERROR:
                                    onError(mId, "ERROR_FILE_ERROR", null);
                                    break;
                                case DownloadManager.ERROR_UNHANDLED_HTTP_CODE:
                                    onError(mId, "ERROR_UNHANDLED_HTTP_CODE", null);
                                    break;
                                case DownloadManager.ERROR_HTTP_DATA_ERROR:
                                    onError(mId, "ERROR_HTTP_DATA_ERROR", null);
                                    break;
                                case DownloadManager.ERROR_TOO_MANY_REDIRECTS:
                                    onError(mId, "ERROR_TOO_MANY_REDIRECTS", null);
                                    break;
                                case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                                    onError(mId, "ERROR_INSUFFICIENT_SPACE", null);
                                    break;
                                case DownloadManager.ERROR_DEVICE_NOT_FOUND:
                                    onError(mId, "ERROR_DEVICE_NOT_FOUND", null);
                                    break;
                                case DownloadManager.ERROR_CANNOT_RESUME:
                                    onError(mId, "ERROR_CANNOT_RESUME", null);
                                    break;
                                case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
                                    onError(mId, "ERROR_FILE_ALREADY_EXISTS", null);
                                    break;
                            }
                        } catch (CursorIndexOutOfBoundsException e) {
                            onError(mId, "ERROR_UNKNOWN", e);
                        }
                    });
                    return null;
                } else if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    mHandler.post(() -> onDownloadCompleted(mId, cursor));
                    return null;
                } else {
                    final int finalDownloadedSoFar = downloadedSoFar;
                    mHandler.post(() -> onProgressUpdate(mId, status, finalDownloadedSoFar));
                }
            } else {
                mHandler.post(() -> onError(mId, "ERROR_DOWNLOAD_NOT_FOUND", null));
                return null;
            }

            // wait 500 milliseconds
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {}
        }

        return null;
    }

    @Override
    protected final void onPreExecute() {}

    @Override
    protected final void onProgressUpdate(Long... values) {}

    @Override
    protected final void onPostExecute(Void aVoid) {
        detach();
    }

    @Override
    protected final void onCancelled() {}
}
