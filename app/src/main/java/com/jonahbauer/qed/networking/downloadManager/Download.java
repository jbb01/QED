package com.jonahbauer.qed.networking.downloadManager;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jonahbauer.qed.networking.Feature;

import java.io.File;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Download extends AsyncTask<Void, Void, Void> {
    private static final ConcurrentHashMap<Long, Download> DOWNLOADS = new ConcurrentHashMap<>();
    private static DownloadManager DOWNLOAD_MANAGER;

    private final Feature mFeature;
    private final Uri mSourceUri;
    private final File mTargetFile;
    private final Map<String, List<String>> mHeaderMap;
    private final String mNotificationTitle;
    private final String mNotificationDescription;

    private DownloadListener mListener;

    private long mId = -1;

    public static void init(Context context) {
        DOWNLOAD_MANAGER = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
    }

    public static void startDownload(@NonNull Download download) {
        download.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
    }

    public static void stopDownload(@NonNull Download download) {
        download.cancel(true);
        DOWNLOAD_MANAGER.remove(download.mId);
    }

    @Nullable
    static Download getDownload(long id) {
        return DOWNLOADS.get(id);
    }

    public Download(long downloadId) {
        this.mId = downloadId;
        this.mListener = null;
        this.mFeature = null;
        this.mSourceUri = null;
        this.mTargetFile = null;
        this.mHeaderMap = null;
        this.mNotificationTitle = null;
        this.mNotificationDescription = null;
    }

    public Download(@NonNull Feature feature,
                    @NonNull Uri sourceUri,
                    @NonNull File target,
                    @Nullable Map<String, List<String>> headerMap,
                    @NonNull DownloadListener listener,
                    @Nullable String notificationTitle,
                    @Nullable String notificationDescription) {
        this.mFeature = feature;
        this.mSourceUri = sourceUri;
        this.mTargetFile = target;
        this.mListener = listener;
        this.mNotificationTitle = notificationTitle;
        this.mNotificationDescription = notificationDescription;

        if (headerMap == null) headerMap = new HashMap<>();
        this.mHeaderMap = headerMap;

        // add cookies
        try {
            URI uri = new URI(sourceUri.toString());
            CookieHandler.getDefault().get(uri, Collections.emptyMap())
                         .forEach((key, values) ->
                             values.forEach(value ->
                                 mHeaderMap.computeIfAbsent(key, k -> new ArrayList<>()).add(value)
                             )
                         );
        } catch (URISyntaxException | IOException ignored) {}
    }

    @Override
    protected Void doInBackground(Void... voids) {
        if (mFeature == null) return null;

        // create download request
        DownloadManager.Request request = new DownloadManager.Request(mSourceUri);
        mHeaderMap.forEach((key, values) -> values.forEach(value -> request.addRequestHeader(key, value)));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        request.setDestinationUri(Uri.fromFile(mTargetFile));

        if (mNotificationTitle != null) request.setTitle(mNotificationTitle);
        if (mNotificationDescription != null) request.setDescription(mNotificationDescription);

        mId = DOWNLOAD_MANAGER.enqueue(request);

        synchronized (DOWNLOADS) {
            DOWNLOADS.put(mId, this);
        }

        if (mListener != null) mListener.attach(mId, DOWNLOAD_MANAGER);

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
            listener.attach(mId, DOWNLOAD_MANAGER);
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

        Cursor cursor = DOWNLOAD_MANAGER.query(query);
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
