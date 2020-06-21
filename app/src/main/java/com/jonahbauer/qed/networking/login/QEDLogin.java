package com.jonahbauer.qed.networking.login;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.LoginActivity;
import com.jonahbauer.qed.networking.Feature;
import com.jonahbauer.qed.networking.NoNetworkException;

import org.jetbrains.annotations.NotNull;

import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.net.ssl.HttpsURLConnection;

public final class QEDLogin {
    private QEDLogin() {}

    private static void loginChat() throws InvalidCredentialsException, NoNetworkException {
        try {
            SoftReference<Application> applicationReference = Application.getApplicationReference();
            Application application = applicationReference.get();

            if (application == null) {
                Log.e(Application.LOG_TAG_ERROR, "", new Exception("Application is null!"));
                throw new NoNetworkException(new Exception("No Application!"));
            }

            String username = application.loadData(Application.KEY_USERNAME, false);
            String password = application.loadData(Application.KEY_PASSWORD, true);
            String version = application.getString(R.string.chat_version);

            byte[] data = ("username=" + username + "&password=" + password + "&version=" + version).getBytes(StandardCharsets.UTF_8);

            String userid = null;
            String pwhash = null;

            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) new URL(application.getString(R.string.chat_server_login)).openConnection();
            httpsURLConnection.setRequestMethod("POST");
            httpsURLConnection.setDoOutput(true);
            httpsURLConnection.setDoInput(true);
            httpsURLConnection.setInstanceFollowRedirects(false);
            httpsURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpsURLConnection.setRequestProperty("charset", "utf-8");
            httpsURLConnection.setRequestProperty("Content-Length", Integer.toString(data.length));
            httpsURLConnection.setUseCaches(false);

            DataOutputStream dataOutputStream = new DataOutputStream(httpsURLConnection.getOutputStream());
            dataOutputStream.write(data);

            httpsURLConnection.connect();

            List<String> cookies = httpsURLConnection.getHeaderFields().get("Set-Cookie");

            httpsURLConnection.disconnect();
            dataOutputStream.close();

            if (cookies == null) {
                throw new InvalidCredentialsException();
            }

            for (String str : cookies) {
                if (str.startsWith("userid=")) userid = str.split("=")[1].split(";")[0];
                else if (str.startsWith("pwhash=")) pwhash = str.split("=")[1].split(";")[0];
            }

            if (userid == null || pwhash == null)
                throw new InvalidCredentialsException();

            application.saveData(userid, Application.KEY_USERID, false);
            application.saveData(pwhash, Application.KEY_CHAT_PWHASH, true);
        } catch (IOException e) {
            throw new NoNetworkException(e);
        }
    }

    private static void loginDatabase() throws InvalidCredentialsException, NoNetworkException {
        try {
            SoftReference<Application> applicationReference = Application.getApplicationReference();
            Application application = applicationReference.get();

            if (application == null) {
                Log.e(Application.LOG_TAG_ERROR, "", new Exception("Application is null!"));
                throw new NoNetworkException(new Exception("No Application!"));
            }

            String username = application.loadData(Application.KEY_USERNAME, false);
            String password = application.loadData(Application.KEY_PASSWORD, true);

            byte[] data = ("username=" + username + "&password=" + password + "&anmelden=anmelden").getBytes(StandardCharsets.UTF_8);

            String sessionId;
            String sessionId2;
            String sessionCookie;

            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) new URL(application.getString(R.string.database_server_login)).openConnection();
            httpsURLConnection.setRequestMethod("POST");
            httpsURLConnection.setDoOutput(true);
            httpsURLConnection.setDoInput(true);
            httpsURLConnection.setInstanceFollowRedirects(false);
            httpsURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpsURLConnection.setRequestProperty("charset", "utf-8");
            httpsURLConnection.setRequestProperty("Content-Length", Integer.toString(data.length));
            httpsURLConnection.setUseCaches(false);

            DataOutputStream dataOutputStream = new DataOutputStream(httpsURLConnection.getOutputStream());
            dataOutputStream.write(data);

            httpsURLConnection.connect();

            String location = httpsURLConnection.getHeaderField("Location");
            String cookie = httpsURLConnection.getHeaderField("Set-Cookie");

            httpsURLConnection.disconnect();
            dataOutputStream.close();

            if (location == null || cookie == null) {
                throw new InvalidCredentialsException();
            }

            try {
                String[] tmp = cookie.split("=");
                sessionCookie = tmp[0];
                sessionId = tmp[1];
                sessionId2 = location.split("session_id2=")[1];
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new InvalidCredentialsException();
            }

            if (sessionCookie == null || sessionId == null || sessionId2 == null)
                throw new InvalidCredentialsException();

            application.saveData(sessionCookie, Application.KEY_DATABASE_SESSION_COOKIE, true);
            application.saveData(sessionId, Application.KEY_DATABASE_SESSIONID, true);
            application.saveData(sessionId2, Application.KEY_DATABASE_SESSIONID2, true);
        } catch (IOException e) {
            throw new NoNetworkException(e);
        }
    }

    private static void loginGallery() throws InvalidCredentialsException, NoNetworkException {
        try {
            SoftReference<Application> applicationReference = Application.getApplicationReference();
            Application application = applicationReference.get();

            if (application == null) {
                Log.e(Application.LOG_TAG_ERROR, "", new Exception("Application is null!"));
                throw new NoNetworkException(new Exception("No Application!"));
            }

            String username = application.loadData(Application.KEY_USERNAME, false);
            String password = application.loadData(Application.KEY_PASSWORD, true);

            byte[] data = ("username=" + username + "&password=" + password + "&login=Einloggen").getBytes(StandardCharsets.UTF_8);

            String phpsessid = null;
            String userid = null;
            String pwhash = null;

            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) new URL(application.getString(R.string.gallery_server_login)).openConnection();
            httpsURLConnection.setRequestMethod("POST");
            httpsURLConnection.setDoOutput(true);
            httpsURLConnection.setDoInput(true);
            httpsURLConnection.setInstanceFollowRedirects(false);
            httpsURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpsURLConnection.setRequestProperty("charset", "utf-8");
            httpsURLConnection.setRequestProperty("Content-Length", Integer.toString(data.length));
            httpsURLConnection.setUseCaches(false);

            DataOutputStream dataOutputStream = new DataOutputStream(httpsURLConnection.getOutputStream());
            dataOutputStream.write(data);

            httpsURLConnection.connect();

            List<String> cookies = httpsURLConnection.getHeaderFields().get("Set-Cookie");

            httpsURLConnection.disconnect();
            dataOutputStream.close();

            if (cookies == null)
                throw new InvalidCredentialsException();

            for (String str : cookies) {
                if (str.startsWith("PHPSESSID=")) phpsessid = str.split("=")[1].split(";")[0];
                else if (str.startsWith("userid=")) userid = str.split("=")[1].split(";")[0];
                else if (str.startsWith("pwhash=")) pwhash = str.split("=")[1].split(";")[0];
            }

            if (phpsessid == null || userid == null || pwhash == null)
                throw new InvalidCredentialsException();


            application.saveData(pwhash, Application.KEY_GALLERY_PWHASH, true);
            application.saveData(phpsessid, Application.KEY_GALLERY_PHPSESSID, true);
            application.saveData(userid, Application.KEY_USERID, false);
        } catch (java.io.IOException e) {
            throw new NoNetworkException(e);
        }
    }

    /**
     * Tries to login to the specified {@code Feature} using stored username and password.
     * The resulting session tokens will be stored using {@link Application#saveData(String, String, boolean)}
     *
     * @param feature a feature
     * @throws InvalidCredentialsException if username and/or password are invalid
     * @throws NoNetworkException if no network connection could be established
     */
    public static void login(@NonNull Feature feature) throws InvalidCredentialsException, NoNetworkException {
        switch (feature) {
            case CHAT:
                QEDLogin.loginChat();
                break;
            case GALLERY:
                QEDLogin.loginGallery();
                break;
            case DATABASE:
                QEDLogin.loginDatabase();
                break;
        }
    }

    /**
     * Checks whether session tokens required to access the specified {@code Feature} are available.
     *
     * @param feature a feature
     * @return true if all required session tokens are stored, false otherwise
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean checkDataAvailable(@NonNull Feature feature) {
        Application application = Application.getApplicationReference().get();
        if (application == null) throw new NullPointerException("Application not found!");

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

    /**
     * Ensures that session tokens required to access the specified {@code Feature} are available.
     * <p>
     * If the tokens are not yet stored, then a login is attempted.
     * When the login fails the {@link LoginActivity} is shown and a {@link InvalidCredentialsException} is thrown.
     * When the login is successful but the data is still not available an {@link AssertionError} is thrown.
     *
     * @param feature a feature
     * @throws InvalidCredentialsException if data is not stored and login fails
     * @throws NoNetworkException if data is not available and login fails due to a missing network connection
     */
    public static void ensureDataAvailable(@NonNull Feature feature) throws InvalidCredentialsException, NoNetworkException {
        if (!checkDataAvailable(feature)) {
            try {
                login(feature);
                if (!checkDataAvailable(feature)) throw new AssertionError("Data not available after login!");
            } catch (InvalidCredentialsException e) {
                Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
                forceLogin();
                throw e;
            }
        }
    }

    /**
     * Shows the login activity.
     */
    public static void forceLogin() {
        Application application = Application.getApplicationReference().get();
        if (application == null) throw new NullPointerException("Application not found!");

        if (application.getActivity() instanceof LoginActivity) return;

        Intent intent = new Intent(application, LoginActivity.class);
        intent.putExtra(LoginActivity.DONT_START_MAIN, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        application.startActivity(intent);
    }

    /**
     * Replaces wildcards matching the {@code Feature} within the given string.
     * <p>
     *     Possible wildcards are:
     *     <ul>
     *         <li><b>{@link Feature#CHAT}</b>: {@code \{\@userid\}} and {@code \{\@pwhash\}}
     *         <li><b>{@link Feature#DATABASE}</b>: {@code \{\@userid\}}, {@code \{\@sessionid\}}, {@code \{\@sessionid2\}} and {@code \{\@sessioncookie\}}
     *         <li><b>{@link Feature#GALLERY}</b>: {@code \{\@userid\}}, {@code \{\@phpsessid\}} and {@code \{\@pwhash\}}
     *     </ul>
     * </p>
     *
     * @param feature a feature
     * @param string a string containing wildcards
     * @return a string with all recognized wildcards replaced
     */
    @SuppressWarnings("RegExpRedundantEscape") // Android requires these escapes
    public static String formatString(@NonNull Feature feature, @NonNull String string) {
        String userId, pwHash, sessionCookie, sessionId, sessionId2, phpSessionId;
        String out = string;

        Application application = Application.getApplicationReference().get();
        if (application == null) throw new NullPointerException("Application not found!");

        switch (feature) {
            case CHAT:
                userId = application.loadData(Application.KEY_USERID, false);
                pwHash = application.loadData(Application.KEY_CHAT_PWHASH, true);

                out = string.replaceAll("\\{@userid\\}", userId).replaceAll("\\{@pwhash\\}", pwHash);
                break;
            case DATABASE:
                userId = application.loadData(Application.KEY_USERID, false);
                sessionCookie = application.loadData(Application.KEY_DATABASE_SESSION_COOKIE, true);
                sessionId = application.loadData(Application.KEY_DATABASE_SESSIONID, true);
                sessionId2 = application.loadData(Application.KEY_DATABASE_SESSIONID2, true);

                out = string.replaceAll("\\{@userid\\}", userId).replaceAll("\\{@sessionid\\}", sessionId).replaceAll("\\{@sessionid2\\}", sessionId2).replaceAll("\\{@sessioncookie\\}", sessionCookie);
                break;
            case GALLERY:
                userId = application.loadData(Application.KEY_USERID, false);
                phpSessionId = application.loadData(Application.KEY_GALLERY_PHPSESSID, true);
                pwHash = application.loadData(Application.KEY_GALLERY_PWHASH, true);

                out = string.replaceAll("\\{@userid\\}", userId).replaceAll("\\{@phpsessid\\}", phpSessionId).replaceAll("\\{@pwhash\\}", pwHash);
                break;
        }

        return out;
    }

    /**
     * Replaces wildcards matching the {@code Feature} within the given strings.
     * <p>
     *     Possible wildcards are:
     *     <ul>
     *         <li><b>{@link Feature#CHAT}</b>: {@code \{\@userid\}} and {@code \{\@pwhash\}}
     *         <li><b>{@link Feature#DATABASE}</b>: {@code \{\@userid\}}, {@code \{\@sessionid\}}, {@code \{\@sessionid2\}} and {@code \{\@sessioncookie\}}
     *         <li><b>{@link Feature#GALLERY}</b>: {@code \{\@userid\}}, {@code \{\@phpsessid\}} and {@code \{\@pwhash\}}
     *     </ul>
     * </p>
     *
     * @param feature a feature
     * @param strings a array of strings containing wildcards
     * @return a array strings with all recognized wildcards replaced
     */
    @NotNull
    @SuppressWarnings("RegExpRedundantEscape") // Android requires these escapes
    public static String[] formatStrings(@NonNull Feature feature, @NonNull String...strings) {

        Application application = Application.getApplicationReference().get();
        if (application == null) throw new NullPointerException("Application not found!");

        String[] out = new String[strings.length];

        switch (feature) {
            case CHAT: {
                String userId = application.loadData(Application.KEY_USERID, false);
                String pwHash = application.loadData(Application.KEY_CHAT_PWHASH, true);

                for (int i = 0; i < strings.length; i++) {
                    out[i] = strings[i].replaceAll("\\{@userid\\}", userId).replaceAll("\\{@pwhash\\}", pwHash);
                }
                break;
            }
            case DATABASE: {
                String userId = application.loadData(Application.KEY_USERID, false);
                String sessionCookie = application.loadData(Application.KEY_DATABASE_SESSION_COOKIE, true);
                String sessionId = application.loadData(Application.KEY_DATABASE_SESSIONID, true);
                String sessionId2 = application.loadData(Application.KEY_DATABASE_SESSIONID2, true);

                for (int i = 0; i < strings.length; i++) {
                    out[i] = strings[i].replaceAll("\\{@userid\\}", userId).replaceAll("\\{@sessionid\\}", sessionId).replaceAll("\\{@sessionid2\\}", sessionId2).replaceAll("\\{@sessioncookie\\}", sessionCookie);
                }
                break;
            }
            case GALLERY: {
                String userId = application.loadData(Application.KEY_USERID, false);
                String phpSessionId = application.loadData(Application.KEY_GALLERY_PHPSESSID, true);
                String pwHash = application.loadData(Application.KEY_GALLERY_PWHASH, true);

                for (int i = 0; i < strings.length; i++) {
                    out[i] = strings[i].replaceAll("\\{@userid\\}", userId).replaceAll("\\{@phpsessid\\}", phpSessionId).replaceAll("\\{@pwhash\\}", pwHash);
                }
                break;
            }
        }

        return out;
    }

    /**
     * Fills the given map with the required cookies for the specified feature.
     * <p>
     *     The cookies are stored as name value pairs, where the value may still contain
     *     wildcards (see {@link #formatString(Feature, String)}.
     * </p>
     * <p>
     *     Use {@link #loadCookies(Feature, Map)} to replace the wildcards and create a {@code String}
     *     that can be used in HTTP-Header.
     * </p>
     * @param feature a feature
     * @param cookies a map
     */
    public static void addCookies(@NonNull Feature feature, @NonNull final Map<String, String> cookies) {
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

    /**
     * Loads the given map containing cookies into a single {@code String}.
     * <p>
     *     The keys and values may contain wildcard as specified in {@link #formatString(Feature, String)}.
     * </p>
     *
     * @param feature a feature
     * @param cookies a map of cookies
     * @return a single {@code String} that can be used in a HTTP header
     */
    public static String loadCookies(@NonNull Feature feature, @NonNull Map<String,String> cookies) {
        StringBuilder cookieStringBuilder = new StringBuilder();

        String[] parts = cookies.entrySet().stream().flatMap(e -> Stream.of(e.getKey(), e.getValue())).toArray(String[]::new);
        parts = formatStrings(feature, parts);

        for (int i = 0; i < parts.length; i += 2) {
            cookieStringBuilder
                    .append(parts[i])
                    .append('=')
                    .append(parts[i + 1])
                    .append(";");
        }

        return cookieStringBuilder.toString();
    }
}
