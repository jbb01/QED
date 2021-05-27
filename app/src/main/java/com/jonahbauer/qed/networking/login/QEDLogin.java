package com.jonahbauer.qed.networking.login;

import android.content.Intent;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.activities.LoginActivity;
import com.jonahbauer.qed.crypt.PasswordStorage;
import com.jonahbauer.qed.crypt.PasswordUtils;
import com.jonahbauer.qed.networking.Feature;
import com.jonahbauer.qed.networking.NetworkConstants;
import com.jonahbauer.qed.networking.NetworkUtils;
import com.jonahbauer.qed.networking.exceptions.InvalidCredentialsException;
import com.jonahbauer.qed.networking.exceptions.NetworkException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class QEDLogin {

    private static void loginChat() throws InvalidCredentialsException, NetworkException {
        Pair<String, char[]> credentials = loadUsernameAndPassword();

        loginChat(credentials.first, credentials.second);

        PasswordUtils.wipe(credentials.second);
    }

    private static void loginDatabase() throws InvalidCredentialsException, NetworkException {
        Pair<String, char[]> credentials = loadUsernameAndPassword();

        loginDatabase(credentials.first, credentials.second);

        PasswordUtils.wipe(credentials.second);
    }

    private static void loginGallery() throws NetworkException, InvalidCredentialsException {
        Pair<String, char[]> credentials = loadUsernameAndPassword();

        loginGallery(credentials.first, credentials.second);

        PasswordUtils.wipe(credentials.second);
    }

    static void loginChat(String username, char[] password) throws InvalidCredentialsException, NetworkException {
        String version = NetworkConstants.CHAT_VERSION;

        byte[] passwordBytes = PasswordUtils.toBytes(password);
        byte[] data = PasswordUtils.combine(
                String.format("username=%s&version=%s&password=", username, version).getBytes(StandardCharsets.UTF_8),
                passwordBytes
        );
        PasswordUtils.wipe(passwordBytes);

        try {
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) new URL(NetworkConstants.CHAT_SERVER_LOGIN).openConnection();
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

            boolean hasUserId = false, hasPwHash = false;

            for (String str : cookies) {
                if (str.startsWith("userid=")) hasUserId = true;
                else if (str.startsWith("pwhash=")) hasPwHash = true;
            }

            if (!hasUserId || !hasPwHash) {
                throw new InvalidCredentialsException();
            }
        } catch (IOException e) {
            throw new NetworkException(e);
        } finally {
            PasswordUtils.wipe(data);
        }
    }

    static void loginDatabase(String username, char[] password) throws InvalidCredentialsException, NetworkException {
        byte[] data = null;
        try {
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) new URL(NetworkConstants.DATABASE_SERVER_LOGIN).openConnection();
            httpsURLConnection.setRequestMethod("GET");
            httpsURLConnection.setDoInput(true);
            httpsURLConnection.setInstanceFollowRedirects(false);
            httpsURLConnection.connect();

            String response = new String(NetworkUtils.readAllBytes(httpsURLConnection.getInputStream()), StandardCharsets.UTF_8);
            Document responseDoc = Jsoup.parse(response);
            String authenticityToken = responseDoc.select("input[name=authenticity_token]").val();

            httpsURLConnection = (HttpsURLConnection) new URL(NetworkConstants.DATABASE_SERVER_LOGIN).openConnection();
            httpsURLConnection.setRequestMethod("POST");
            httpsURLConnection.setDoInput(true);
            httpsURLConnection.setDoOutput(true);
            httpsURLConnection.setInstanceFollowRedirects(false);
            httpsURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpsURLConnection.setRequestProperty("charset", "utf-8");

            byte[] passwordBytes = PasswordUtils.toBytes(password);
            data = PasswordUtils.combine(
                    String.format("account_name=%s&authenticity_token=%s&password=", username, authenticityToken).getBytes(StandardCharsets.UTF_8),
                    passwordBytes
            );
            PasswordUtils.wipe(passwordBytes);

            DataOutputStream dataOutputStream = new DataOutputStream(httpsURLConnection.getOutputStream());
            dataOutputStream.write(data);

            httpsURLConnection.connect();

            int responseCode = httpsURLConnection.getResponseCode();
            String location = httpsURLConnection.getHeaderField("Location");

            httpsURLConnection.disconnect();

            if (responseCode != 302 || location == null) {
                throw new InvalidCredentialsException();
            }
        } catch (IOException e) {
            throw new NetworkException(e);
        } finally {
            if (data != null) PasswordUtils.wipe(data);
        }
    }

    static void loginGallery(String username, char[] password) throws InvalidCredentialsException, NetworkException {
        byte[] passwordBytes = PasswordUtils.toBytes(password);
        byte[] data = PasswordUtils.combine(
                String.format("username=%s&login=Einloggen&password=", username).getBytes(StandardCharsets.UTF_8),
                passwordBytes
        );
        PasswordUtils.wipe(passwordBytes);

        try {
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) new URL(NetworkConstants.GALLERY_SERVER_LOGIN).openConnection();
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

            boolean hasUserId = false, hasPwHash = false;

            for (String str : cookies) {
                if (str.startsWith("userid=")) hasUserId = true;
                else if (str.startsWith("pwhash=")) hasPwHash = true;
            }

            if (!hasUserId || !hasPwHash) {
                throw new InvalidCredentialsException();
            }
        } catch (java.io.IOException e) {
            throw new NetworkException(e);
        } finally {
            PasswordUtils.wipe(data);
        }
    }

    /**
     * @see #login(Feature, boolean)
     */
    public static void login(@NonNull Feature feature) throws InvalidCredentialsException, NetworkException {
        login(feature, true);
    }

    /**
     * Tries to login to the specified {@code Feature} using stored username and password.
     * The {@link com.jonahbauer.qed.networking.cookies.QEDCookieHandler} will take care of
     * storing session cookies and adding the to following requests.
     *
     * @param feature a feature
     * @throws InvalidCredentialsException if username and/or password are invalid
     * @throws NetworkException if no network connection could be established
     */
    public static void login(@NonNull Feature feature, boolean promptIfNecessary) throws InvalidCredentialsException, NetworkException {
        try {
            // TODO if no password store is enabled switch to promptLogin immediately
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
        } catch (InvalidCredentialsException e) {
            if (promptIfNecessary) promptLogin(feature);

            throw e;
        }
    }

    /**
     * Shows the login activity.
     */
    public static void promptLogin(@NonNull Feature feature) {
        Application application = Application.getApplicationReference().get();
        if (application == null) throw new NullPointerException("Application not found!");

        Intent intent = new Intent(application, LoginActivity.class);
        intent.putExtra(LoginActivity.EXTRA_DONT_START_MAIN, true);
        intent.putExtra(LoginActivity.EXTRA_FEATURE, feature);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        application.startActivity(intent);
    }

    private static Pair<String, char[]> loadUsernameAndPassword() throws InvalidCredentialsException {
        Pair<String, char[]> out = PasswordStorage.loadUsernameAndPassword();

        if (out.first == null || out.second == null) {
            throw new InvalidCredentialsException("Could not obtain credentials.");
        }

        return out;
    }
}
