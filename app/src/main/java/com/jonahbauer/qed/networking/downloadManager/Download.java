package com.jonahbauer.qed.networking.downloadManager;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.networking.Feature;
import com.jonahbauer.qed.networking.NoNetworkException;
import com.jonahbauer.qed.networking.login.InvalidCredentialsException;
import com.jonahbauer.qed.networking.login.QEDLogin;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Download extends AsyncTask<Void, Void, Void> {
    private static final DownloadManager sDownloadManager;
    private static final ConcurrentHashMap<Long, Download> sDownloads;

    private final Application mApplication;

    private final Feature mFeature;
    private final Uri mSourceUri;
    private final File mTargetFile;
    private final Map<String,String> mCookies;
    private final String mNotificationTitle;
    private final String mNotificationDescription;

    private DownloadListener mListener;

    private long mId = -1;

    static {
        sDownloads = new ConcurrentHashMap<>();

        SoftReference<Application> applicationReference = Application.getApplicationReference();
        Application application = applicationReference.get();

        if (application != null)
            sDownloadManager = (DownloadManager) application.getSystemService(Context.DOWNLOAD_SERVICE);
        else
            sDownloadManager = null;
    }

    public static void startDownload(@NonNull Download download) {
        download.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
    }

    public static void stopDownload(@NonNull Download download) {
        download.cancel(true);
        sDownloadManager.remove(download.mId);
    }

    @Nullable
    static Download getDownload(long id) {
        return sDownloads.get(id);
    }

    public Download(long downloadId) {
        this.mId = downloadId;
        this.mListener = null;
        this.mFeature = null;
        this.mSourceUri = null;
        this.mTargetFile = null;
        this.mCookies = null;
        this.mApplication = null;
        this.mNotificationTitle = null;
        this.mNotificationDescription = null;
    }

    public Download(@NonNull Feature feature, @NonNull Uri sourceUri, @NonNull File target, @Nullable Map<String, String> cookies, @NonNull DownloadListener listener, @Nullable String notificationTitle, @Nullable String notificationDescription) {
        SoftReference<Application> applicationReference = Application.getApplicationReference();
        this.mApplication = applicationReference.get();

        this.mFeature = feature;
        this.mSourceUri = sourceUri;
        this.mTargetFile = target;
        this.mListener = listener;
        this.mNotificationTitle = notificationTitle;
        this.mNotificationDescription = notificationDescription;

        if (cookies == null) cookies = new HashMap<>();
        this.mCookies = cookies;

        if (this.mApplication == null) {
            listener.onError(mId, null, new NullPointerException("Application reference points to a null object."));
            return;
        }

        QEDLogin.addCookies(feature, this.mCookies);
    }

    @Override
    protected Void doInBackground(Void... voids) {
        if (mFeature == null) return null;

        try {
            // ensure logged in
            QEDLogin.ensureDataAvailable(mFeature);

            // create download request
            DownloadManager.Request request = new DownloadManager.Request(mSourceUri);
            request.addRequestHeader("Cookie", QEDLogin.loadCookies(mFeature, mCookies));
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
            request.setDestinationUri(Uri.fromFile(mTargetFile));

            if (mNotificationTitle != null) request.setTitle(mNotificationTitle);
            if (mNotificationDescription != null) request.setDescription(mNotificationDescription);

            mId = sDownloadManager.enqueue(request);

            synchronized (sDownloads) {
                sDownloads.put(mId, this);
            }

            if (mListener != null) mListener.attach(mId, sDownloadManager);
        } catch (NoNetworkException e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
            if (mListener != null) mListener.onError(mId, "ERROR_NETWORK_ERROR", e);
        } catch (InvalidCredentialsException e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
            if (mListener != null) mListener.onError(mId, "ERROR_INVALID_CREDENTIALS", e);
        }

        return null;
    }

    /**
     * Attaches the given listener to this download. For this point onward it will receive all updates.
     * The load listener will be detatched.
     *
     * @param listener a listener
     */
    public void attachListener(@NonNull DownloadListener listener) {
        if (this.mListener == null || !this.mListener.equals(listener))  {
            if (this.mListener != null) this.mListener.detach();

            this.mListener = listener;
            listener.attach(mId, sDownloadManager);
        }
    }

    /**
     * Detaches the given listener from this download. It will no longer receive updates.
     *
     * @param listener a listener
     */
    public void detachListener(@NonNull DownloadListener listener) {
        listener.detach();
    }

    public int getDownloadStatus() {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(mId);

        Cursor cursor = sDownloadManager.query(query);
        if (cursor.moveToFirst()) { // should always be true
            int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            return cursor.getInt(statusIndex);
        }

        return -1;
    }

    public long getDownloadId() {
        return mId;
    }

    public DownloadListener getListener() {
        return mListener;
    }
}
