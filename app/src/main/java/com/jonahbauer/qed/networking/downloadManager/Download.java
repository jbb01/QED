package com.jonahbauer.qed.networking.downloadManager;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LongSparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.activities.LoginActivity;
import com.jonahbauer.qed.networking.NoNetworkException;
import com.jonahbauer.qed.networking.login.InvalidCredentialsException;
import com.jonahbauer.qed.networking.login.QEDLogin;

import org.jetbrains.annotations.Contract;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

public class Download extends AsyncTask<Void, Void, Void> {
    private static final Map<String, String> credentials;
    private static final DownloadManager downloadManager;

    private static final LongSparseArray<Download> downloads;

    private final Application application;

    private final Feature feature;
    private final Uri uri;
    private final File file;
    private final Map<String,String> cookies;
    private final String notificationTitle;
    private final String notificationDescription;

    private DownloadListener listener;

    private boolean forcedLogin = false;

    private long id = -1;


    static {
        credentials = new HashMap<>();
        downloads = new LongSparseArray<>();

        SoftReference<Application> applicationReference = Application.getApplicationReference();
        Application application = applicationReference.get();

        if (application != null)
            downloadManager = (DownloadManager) application.getSystemService(Context.DOWNLOAD_SERVICE);
        else
            downloadManager = null;
    }

    public static void startDownload(@NonNull Download download) {
        download.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
    }

    public static void stopDownload(@NonNull Download download) {
        download.cancel(true);
        downloadManager.remove(download.id);
    }

    @Nullable
    static Download getDownload(long id) {
        return downloads.get(id);
    }

    @Nullable
    public static Uri getCompletedDownload(long id) {
        return downloadManager.getUriForDownloadedFile(id);
    }


    public Download(long downloadId) {
        this.id = downloadId;
        this.listener = null;
        this.feature = null;
        this.uri = null;
        this.file = null;
        this.cookies = null;
        this.application = null;
        this.notificationTitle = null;
        this.notificationDescription = null;
    }

    public Download(@NonNull Feature feature, @NonNull Uri uri, @NonNull File target, @Nullable Map<String, String> cookies, @NonNull DownloadListener listener, @Nullable String notificationTitle, @Nullable String notificationDescription) {
        SoftReference<Application> applicationReference = Application.getApplicationReference();
        application = applicationReference.get();

        this.feature = feature;
        this.uri = uri;

        if (cookies == null)
            this.cookies = new HashMap<>();
        else
            this.cookies = cookies;

        this.file = target;
        this.listener = listener;
        this.notificationTitle = notificationTitle;
        this.notificationDescription = notificationDescription;

        if (application == null) {
            listener.onError(id, null, new AssertionError("Application reference points to a null object."));
            return;
        }

        // add required cookies
        switch (feature) {
            case CHAT:
                this.cookies.put("userid", "{@userid}");
                this.cookies.put("pwhash", "{@pwhash}");
                break;
            case GALLERY:
                this.cookies.put("userid", "{@userid}");
                this.cookies.put("PHPSESSID", "{@phpsessid}");
                this.cookies.put("pwhash", "{@pwhash}");
                break;
            case DATABASE:
                this.cookies.put("{@sessioncookie}", "{@sessionid}");
                break;
        }
    }

    @Override
    protected Void doInBackground(Void... voids) {
        if (feature == null) return null;

        // ensure logged in
        if (!dataAvailable(feature) && !login()) return null;
        if (!dataAvailable(feature)) throw new AssertionError();

        // create download request
        DownloadManager.Request request = new DownloadManager.Request(uri);

        StringBuilder cookieStringBuilder = new StringBuilder();
        for (String header : cookies.keySet()) {
            cookieStringBuilder.append(formatString(header, feature)).append("=").append(formatString(cookies.get(header),feature)).append(";");
        }

        if (notificationTitle != null) request.setTitle(notificationTitle);
        if (notificationDescription != null) request.setDescription(notificationDescription);
        request.addRequestHeader("Cookie", cookieStringBuilder.toString());

        Uri uri = Uri.fromFile(file);
        request.setDestinationUri(uri);

        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);

        id = downloadManager.enqueue(request);

        synchronized (downloads) {
            downloads.put(id, this);
        }

        if (listener != null) listener.attach(id, downloadManager);

        return null;
    }


    public void attachListener(@NonNull DownloadListener listener) {
        if (this.listener == null || !this.listener.equals(listener))  {
            if (this.listener != null) this.listener.detach();

            this.listener = listener;
            listener.attach(id, downloadManager);
        }
    }

    public void detachListener(@NonNull DownloadListener listener) {
        listener.detach();
    }


    public int getDownloadStatus() {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(id);

        Cursor cursor = downloadManager.query(query);
        if (cursor.moveToFirst()) { // should always be true
            int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            return cursor.getInt(statusIndex);
        }

        return -1;
    }

    public long getDownloadId() {
        return id;
    }

    public DownloadListener getListener() {
        return listener;
    }

    @Contract("null, _ -> !null")
    @SuppressWarnings("RegExpRedundantEscape")
    private String formatString(@Nullable String string, @NonNull Feature feature) {
        if (string == null) return "";

        String userId, pwHash, sessionCookie, sessionId, sessionId2, phpSessionId;
        String out = string;
        switch (feature) {
            case CHAT:
                userId = credentials.getOrDefault(Application.KEY_USERID, null);
                if (userId == null) {
                    userId = application.loadData(Application.KEY_USERID, false);
                    credentials.put(Application.KEY_USERID, userId);
                }

                pwHash = credentials.getOrDefault(Application.KEY_CHAT_PWHASH, null);
                if (pwHash == null) {
                    pwHash = application.loadData(Application.KEY_CHAT_PWHASH, true);
                    credentials.put(Application.KEY_CHAT_PWHASH, pwHash);
                }

                out = string.replaceAll("\\{\\@userid\\}", userId).replaceAll("\\{\\@pwhash\\}", pwHash);
                break;
            case DATABASE:
                userId = credentials.getOrDefault(Application.KEY_USERID, null);
                if (userId == null) {
                    userId = application.loadData(Application.KEY_USERID, false);
                    credentials.put(Application.KEY_USERID, userId);
                }

                sessionCookie = credentials.getOrDefault(Application.KEY_DATABASE_SESSION_COOKIE, null);
                if (sessionCookie == null) {
                    sessionCookie = application.loadData(Application.KEY_DATABASE_SESSION_COOKIE, true);
                    credentials.put(Application.KEY_DATABASE_SESSION_COOKIE, sessionCookie);
                }

                sessionId = credentials.getOrDefault(Application.KEY_DATABASE_SESSIONID, null);
                if (sessionId == null) {
                    sessionId = application.loadData(Application.KEY_DATABASE_SESSIONID, true);
                    credentials.put(Application.KEY_DATABASE_SESSIONID, sessionId);
                }

                sessionId2 = credentials.getOrDefault(Application.KEY_DATABASE_SESSIONID2, null);
                if (sessionId2 == null) {
                    sessionId2 = application.loadData(Application.KEY_DATABASE_SESSIONID2, true);
                    credentials.put(Application.KEY_DATABASE_SESSIONID2, sessionId2);
                }

                out = string.replaceAll("\\{\\@userid\\}", userId).replaceAll("\\{\\@sessionid\\}", sessionId).replaceAll("\\{\\@sessionid2\\}", sessionId2).replaceAll("\\{\\@sessioncookie\\}", sessionCookie);
                break;
            case GALLERY:
                userId = credentials.getOrDefault(Application.KEY_USERID, null);
                if (userId == null) {
                    userId = application.loadData(Application.KEY_USERID, false);
                    credentials.put(Application.KEY_USERID, userId);
                }

                phpSessionId = credentials.getOrDefault(Application.KEY_GALLERY_PHPSESSID, null);
                if (phpSessionId == null) {
                    phpSessionId = application.loadData(Application.KEY_GALLERY_PHPSESSID, true);
                    credentials.put(Application.KEY_GALLERY_PHPSESSID, phpSessionId);
                }

                pwHash = credentials.getOrDefault(Application.KEY_GALLERY_PWHASH, null);
                if (pwHash == null) {
                    pwHash = application.loadData(Application.KEY_GALLERY_PWHASH, true);
                    credentials.put(Application.KEY_GALLERY_PWHASH, pwHash);
                }

                out = string.replaceAll("\\{\\@userid\\}", userId).replaceAll("\\{\\@phpsessid\\}", phpSessionId).replaceAll("\\{\\@pwhash\\}", pwHash);
                break;
        }

        return out;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean dataAvailable(@NonNull Feature feature) {
        String userId, pwHash, sessionCookie, sessionId, sessionId2, phpSessionId;
        switch (feature) {
            case CHAT:
                userId = application.loadData(Application.KEY_USERID, false);
                pwHash = application.loadData(Application.KEY_CHAT_PWHASH, true);

                if (userId == null || pwHash == null) return false;
                break;
            case DATABASE:
                sessionCookie = application.loadData(Application.KEY_DATABASE_SESSION_COOKIE, true);
                sessionId = application.loadData(Application.KEY_DATABASE_SESSIONID, true);
                sessionId2 = application.loadData(Application.KEY_DATABASE_SESSIONID2, true);

                if (sessionCookie == null || sessionId == null || sessionId2 == null) return false;
                break;
            case GALLERY:
                userId = application.loadData(Application.KEY_USERID, false);
                pwHash = application.loadData(Application.KEY_GALLERY_PWHASH, true);
                phpSessionId = application.loadData(Application.KEY_GALLERY_PHPSESSID, true);

                if (userId == null || pwHash == null || phpSessionId == null) return false;
                break;
        }

        return true;
    }

    private boolean login() {
        credentials.clear();
        try {
            switch (feature) {
                case CHAT:
                    QEDLogin.loginChat();
                    break;
                case DATABASE:
                    QEDLogin.loginDatabase();
                    break;
                case GALLERY:
                    QEDLogin.loginGallery();
                    break;
            }
        } catch (NoNetworkException e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
            if (listener != null) listener.onError(id, "ERROR_NETWORK_ERROR", e);
            return false;
        } catch (InvalidCredentialsException e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
            forceLogin(e);
            return false;
        }
        return true;
    }

    private void forceLogin(InvalidCredentialsException e) {
        if (!forcedLogin) {
            forcedLogin = true;
            Intent intent = new Intent(application, LoginActivity.class);
            intent.putExtra(LoginActivity.DONT_START_MAIN, true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            application.startActivity(intent);
        }

        if (listener != null) listener.onError(id, "ERROR_INVALID_CREDENTIALS", e);
    }

    public enum Feature {
        CHAT, DATABASE, GALLERY
    }
}
