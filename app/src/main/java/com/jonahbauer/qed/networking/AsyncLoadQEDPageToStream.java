package com.jonahbauer.qed.networking;

import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.activities.LoginActivity;
import com.jonahbauer.qed.networking.login.InvalidCredentialsException;
import com.jonahbauer.qed.networking.login.QEDLogin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class AsyncLoadQEDPageToStream extends AsyncTask<Void, Long, Boolean> {
    private OutputStream outputStream;
    private InputStream in;
    private Feature feature;
    private String url;
    private QEDPageStreamReceiver receiver;
    private String tag;
    private static Map<String, String> credentials;

    private Application application;

    public static boolean forcedLogin;

    static {
        credentials = new HashMap<>();
    }

    AsyncLoadQEDPageToStream(@NonNull Feature feature, @NonNull String url, @NonNull QEDPageStreamReceiver receiver, String tag, @NonNull OutputStream outputStream) {
        this.feature = feature;
        this.url = url;
        this.receiver = receiver;
        this.tag = tag;
        this.outputStream = outputStream;

        SoftReference<Application> applicationReference = Application.getApplicationReference();
        application = applicationReference.get();
    }


    @Override
    protected Boolean doInBackground(Void... voids) {
//        if (!dataAvailable(feature)) {
//    if (!login()) return false;
//    else if (!dataAvailable(feature)) throw new AssertionError();
//}

        try {
            HttpsURLConnection httpsURLConnection = createConnection();
            if (isCancelled()) return false;
            httpsURLConnection.connect();

            String location = httpsURLConnection.getHeaderField("Location");
            boolean loginError = location != null && location.startsWith("account");
            if (loginError) {
                httpsURLConnection.disconnect();

                if (login()) {
                    httpsURLConnection = createConnection();
                    if (isCancelled()) return false;
                    httpsURLConnection.connect();

                    location = httpsURLConnection.getHeaderField("Location");
                    loginError = location != null && location.startsWith("account");
                    if (loginError)
                        throw new InvalidCredentialsException(null);

                    in = httpsURLConnection.getInputStream();
                    try {
                        copyStream(in, outputStream, httpsURLConnection.getContentLength());
                    } catch (IOException e) {
                        receiver.onError(tag, null, e);
                        return false;
                    }
                    in.close();
                    outputStream.close();

                    httpsURLConnection.disconnect();

                    return true;
                } else {
                    return false;
                }
            }

            in = httpsURLConnection.getInputStream();
            try {
                copyStream(in, outputStream, httpsURLConnection.getContentLength());
            } catch (IOException e) {
                receiver.onError(tag, null, e);
                return false;
            }
            in.close();
            outputStream.close();

            httpsURLConnection.disconnect();

            return true;
        } catch (IOException e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
            receiver.onError(tag, null, e);
            return false;
        } catch (InvalidCredentialsException e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
            forceLogin();
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean b) {
        if (b != null && b)
            receiver.onPageReceived(tag, null);
    }

    @Override
    protected void onProgressUpdate(Long... values) {
        receiver.onProgressUpdate(tag, values[0], values[1]);
    }

    private HttpsURLConnection createConnection() throws IOException {
        ensureCredentialAvailability();
        HttpsURLConnection httpsURLConnection = (HttpsURLConnection) (new URL(formatString(url, feature)).openConnection());
        httpsURLConnection.setRequestMethod("GET");
        httpsURLConnection.setDoInput(true);
        httpsURLConnection.setInstanceFollowRedirects(false);
        httpsURLConnection.setUseCaches(false);

        StringBuilder cookieStringBuilder = new StringBuilder();
        switch (feature) {
            case CHAT:
                cookieStringBuilder
                        .append("userid=").append(credentials.get(Application.KEY_USERID))
                        .append("; pwhash=").append(credentials.get(Application.KEY_CHAT_PWHASH))
                        .append(";");
                break;
            case GALLERY:
                cookieStringBuilder
                        .append("userid=").append(credentials.get(Application.KEY_USERID))
                        .append("; pwhash=").append(credentials.get(Application.KEY_GALLERY_PWHASH))
                        .append("; phpsessid=").append(credentials.get(Application.KEY_GALLERY_PHPSESSID))
                        .append(";");
                break;
        }

        httpsURLConnection.setRequestProperty("Cookie", cookieStringBuilder.toString());

        return httpsURLConnection;
    }

    private void ensureCredentialAvailability() {
        String userId, pwHash, phpSessionId;
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
                break;
        }
    }

    @SuppressWarnings("RegExpRedundantEscape")
    private String formatString(String string, Feature feature) {
        String userId, pwHash, phpSessionId;
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

    private boolean login() {
        credentials = new HashMap<>();
        try {
            switch (feature) {
                case CHAT:
                    QEDLogin.loginChat();
                    break;
                case GALLERY:
                    QEDLogin.loginGallery();
                    break;
            }
        } catch (NoNetworkException e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
            receiver.onError(tag, QEDPageReceiver.REASON_NETWORK, e);
            return false;
        } catch (InvalidCredentialsException e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
            forceLogin();
            return false;
        }
        return true;
    }

    private void forceLogin() {
        if (!forcedLogin) {
            forcedLogin = true;
            Intent intent = new Intent(application, LoginActivity.class);
            intent.putExtra(LoginActivity.DONT_START_MAIN, true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            application.startActivity(intent);
        }
        cancel(true);
        receiver.onError(tag, QEDPageReceiver.REASON_UNABLE_TO_LOG_IN, null);
    }

    private void copyStream(InputStream in, OutputStream out, long contentLength) throws IOException {
        byte[] buffer = new byte[4 * 1024]; // 4 kilobyte
        long count = 0;
        int n;
        int i = 0;
        while (-1 != (n = in.read(buffer)) && !isCancelled()) {
            out.write(buffer, 0, n);
            count += n;
            i++;
            if (i == 64) { // 64*4 = 256 kilobyte
                i = 0;
                publishProgress(count, contentLength);
            }
        }
    }

    @Override
    protected void onCancelled() {
        try {
            if (outputStream != null)
                outputStream.close();
            if (in != null)
                in.close();
        } catch (IOException e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
        }
    }

    public enum Feature {
        CHAT, GALLERY
    }
}
