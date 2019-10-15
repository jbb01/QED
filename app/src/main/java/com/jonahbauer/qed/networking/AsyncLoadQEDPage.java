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
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Function;

import javax.net.ssl.HttpsURLConnection;

public class AsyncLoadQEDPage<T> extends AsyncTask<Void, Void, String> {
    private Feature feature;
    private Application application;
    private String url;
    private String tag;
    private QEDPageReceiver<T> receiver;
    private Map<String,String> cookies;
    private Function<String, T> postExecute;

    private static Map<String, String> credentials;

    public static boolean forcedLogin;

    static {
        credentials = new HashMap<>();
    }

    /*
        Loads the webpage from url. url might contain the wildcards "{@userid}", "{@pwhash}", "{@sessionid}", "{@sessionid2}"
     */
    AsyncLoadQEDPage(@NonNull Feature feature, @NonNull String url, @NonNull QEDPageReceiver<T> receiver, String tag, @NonNull Function<String, T> postExecute, @NonNull Map<String, String> cookies) {
        this.feature = feature;
        this.url = url;
        this.tag = tag;
        this.receiver = receiver;
        this.postExecute = postExecute;

        this.cookies = cookies;

        application = Application.getContext();

        switch (feature) {
            case CHAT:
                cookies.put("userid", "{@userid}");
                cookies.put("pwhash", "{@pwhash}");
                break;
            case GALLERY:
                cookies.put("userid", "{@userid}");
                cookies.put("PHPSESSID", "{@phpsessid}");
                cookies.put("pwhash", "{@pwhash}");
                break;
            case DATABASE:
                cookies.put("{@sessioncookie}", "{@sessionid}");
                break;
        }
    }

    @Override
    protected String doInBackground(Void... voids) {
        if (!dataAvailable(feature)) {
            if (!login()) return null;
        }

        if (!dataAvailable(feature)) throw new AssertionError();

        try {
            String out = null;

            HttpsURLConnection httpsURLConnection = createConnection();
            httpsURLConnection.connect();

            String location = httpsURLConnection.getHeaderField("Location");
            boolean loginError = false;
            if (feature == Feature.CHAT || feature == Feature.GALLERY) loginError = location != null && location.startsWith("account");
            else if (feature == Feature.DATABASE) {
                out = readPage(httpsURLConnection);
                loginError = out.contains("nicht eingeloggt");
            }
            if (loginError) {
                httpsURLConnection.disconnect();

                if (login()) {
                    httpsURLConnection = createConnection();
                    httpsURLConnection.connect();

                    location = httpsURLConnection.getHeaderField("Location");
                    loginError = false;
                    if (feature == Feature.CHAT || feature == Feature.GALLERY) loginError = location != null && location.startsWith("account");
                    else if (feature == Feature.DATABASE) {
                        out = readPage(httpsURLConnection);
                        loginError = out.contains("nicht eingeloggt");
                    }
                    if (loginError)
                        throw new InvalidCredentialsException(null);

                    if (feature != Feature.DATABASE) out = readPage(httpsURLConnection);
                    httpsURLConnection.disconnect();

                    return out;
                } else {
                    return null;
                }
            }

            if (feature != Feature.DATABASE) out = readPage(httpsURLConnection);
            httpsURLConnection.disconnect();

            return out;
        } catch (IOException e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
            receiver.onNetworkError(tag);
            return null;
        } catch (InvalidCredentialsException e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
            forceLogin();
            return null;
        }
    }

    @Override
    protected void onPostExecute(String s) {
        if (s != null)
            receiver.onPageReceived(tag, postExecute.apply(s));
    }

    private HttpsURLConnection createConnection() throws IOException {
        HttpsURLConnection httpsURLConnection = (HttpsURLConnection) (new URL(formatString(url, feature)).openConnection());
        httpsURLConnection.setRequestMethod("POST");
        httpsURLConnection.setInstanceFollowRedirects(false);
        httpsURLConnection.setUseCaches(false);

        StringBuilder cookieStringBuilder = new StringBuilder();
        for (String header : cookies.keySet()) {
            cookieStringBuilder.append(formatString(header, feature)).append("=").append(formatString(cookies.getOrDefault(header, "null"),feature)).append(";");
        }

        httpsURLConnection.setRequestProperty("Cookie", cookieStringBuilder.toString());

        return httpsURLConnection;
    }

    @SuppressWarnings("RegExpRedundantEscape")
    private String formatString(String string, Feature feature) {
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
    private boolean dataAvailable(Feature feature) {
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
        credentials = new HashMap<>();
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
            receiver.onNetworkError(tag);
            return false;
        } catch (InvalidCredentialsException e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
            forceLogin();
            return false;
        }
        return true;
    }

    private String readPage(HttpsURLConnection httpsURLConnection) throws IOException {
        StringBuilder out = new StringBuilder();
        Scanner scanner = new Scanner(httpsURLConnection.getInputStream());

        while (scanner.hasNextLine()) {
            out.append(scanner.nextLine());
        }

        scanner.close();

        return out.toString();
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
        receiver.onNetworkError(tag);
    }

    public enum Feature {
        CHAT, DATABASE, GALLERY
    }
}
