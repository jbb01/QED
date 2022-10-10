package com.jonahbauer.qed.networking.login;

import android.app.Activity;
import android.app.PendingIntent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.NavDeepLinkBuilder;
import androidx.navigation.Navigation;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.MainDirections;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.MainActivity;
import com.jonahbauer.qed.activities.mainFragments.LoginFragmentArgs;
import com.jonahbauer.qed.crypt.PasswordStorage;
import com.jonahbauer.qed.crypt.PasswordUtils;
import com.jonahbauer.qed.networking.Feature;
import com.jonahbauer.qed.networking.NetworkConstants;
import com.jonahbauer.qed.networking.NetworkUtils;
import com.jonahbauer.qed.networking.async.QEDPageReceiver;
import com.jonahbauer.qed.networking.cookies.QEDCookieHandler;
import com.jonahbauer.qed.networking.exceptions.InvalidCredentialsException;
import com.jonahbauer.qed.networking.exceptions.NetworkException;
import com.jonahbauer.qed.util.Preferences;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class QEDLogin {
    private static final String LOG_TAG = QEDLogin.class.getName();

    /**
     * Performs a login to the chat using stored username and password.
     * The authentication cookie will be handled by the {@link QEDCookieHandler}.
     */
    private static void loginChat() throws InvalidCredentialsException, NetworkException {
        Pair<String, char[]> credentials = loadUsernameAndPassword();

        loginChat(credentials.first, credentials.second);

        PasswordUtils.wipe(credentials.second);
    }

    /**
     * Performs a login to the database using stored username and password.
     * The authentication cookie will be handled by the {@link QEDCookieHandler}.
     */
    private static void loginDatabase() throws InvalidCredentialsException, NetworkException {
        Pair<String, char[]> credentials = loadUsernameAndPassword();

        loginDatabase(credentials.first, credentials.second);

        PasswordUtils.wipe(credentials.second);
    }

    /**
     * Performs a login to the gallery using stored username and password.
     * The authentication cookie will be handled by the {@link QEDCookieHandler}.
     */
    private static void loginGallery() throws NetworkException, InvalidCredentialsException {
        Pair<String, char[]> credentials = loadUsernameAndPassword();

        loginGallery(credentials.first, credentials.second);

        PasswordUtils.wipe(credentials.second);
    }

    /**
     * Performs a login to the chat using the specified username and password.
     * The authentication cookie will be handled by the {@link QEDCookieHandler}.
     */
    private static void loginChat(String username, char[] password) throws InvalidCredentialsException, NetworkException {
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

    /**
     * Performs a login to the database using the specified username and password.
     * The authentication cookie will be handled by the {@link QEDCookieHandler}.
     */
    private static void loginDatabase(String username, char[] password) throws InvalidCredentialsException, NetworkException {
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

    /**
     * Performs a login to the gallery using the specified username and password.
     * The authentication cookie will be handled by the {@link QEDCookieHandler}.
     */
    private static void loginGallery(String username, char[] password) throws InvalidCredentialsException, NetworkException {
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
     * Performs a login to the specified feature using the specified username and password.
     * The authentication cookie will be handled by the {@link QEDCookieHandler}.
     */
    private static void login(String username, char[] password, Feature feature) throws InvalidCredentialsException, NetworkException {
        switch (feature) {
            default:
            case CHAT:
                loginChat(username, password);
                break;
            case GALLERY:
                loginGallery(username, password);
                break;
            case DATABASE:
                loginDatabase(username, password);
                break;
        }
    }

    /**
     * Asynchronously performs a login to the specified feature using the specified username and password.
     * The authentication cookie will be handled by the {@link QEDCookieHandler}.
     */
    public static Disposable loginAsync(String username, char[] password, Feature feature, QEDPageReceiver<Boolean> listener) {
        var observable = Single.fromCallable(() -> {
            try {
                login(username, password, feature);

                if (Preferences.getGeneral().isRememberMe()) {
                    PasswordStorage.saveUsernameAndPassword(username, password);
                }
            } finally {
                PasswordUtils.wipe(password);
            }
            return true;
        });

        return observable.subscribeOn(Schedulers.io())
                         .observeOn(AndroidSchedulers.mainThread())
                         .subscribe(
                                 listener::onResult,
                                 err -> listener.onError(false, err)
                         );
    }

    public static Disposable loginAsync(@NonNull Feature feature, QEDPageReceiver<Boolean> listener) {
        var observable = Single.fromCallable(() -> {
            login(feature, false);
            return true;
        });

        return observable.subscribeOn(Schedulers.io())
                         .observeOn(AndroidSchedulers.mainThread())
                         .doOnError(err -> {
                             if (err instanceof InvalidCredentialsException) {
                                 promptLogin(feature);
                             }
                         })
                         .subscribe(
                                 listener::onResult,
                                 err -> listener.onError(false, err)
                         );
    }

    /**
     * @see #login(Feature, boolean)
     */
    public static void login(@NonNull Feature feature) throws InvalidCredentialsException, NetworkException {
        login(feature, true);
    }

    /**
     * Tries to login to the specified {@code Feature} using stored username and password.
     * The {@link QEDCookieHandler} will take care of
     * storing session cookies and adding the to following requests.
     *
     * @param feature a feature
     * @throws InvalidCredentialsException if username and/or password are invalid
     * @throws NetworkException if no network connection could be established
     */
    public static void login(@NonNull Feature feature, boolean promptIfNecessary) throws InvalidCredentialsException, NetworkException {
        try {
            if (!Preferences.getGeneral().isRememberMe()) throw new InvalidCredentialsException("Remember me is deactivated.");
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
    @SneakyThrows
    public static void promptLogin(@NonNull Feature feature) {
        Application application = Application.getApplicationReference().get();
        if (application == null) throw new NullPointerException("Application not found!");

        Activity activity = application.getActivity();
        if (activity instanceof MainActivity) {
            new Handler(Looper.getMainLooper()).post(() -> {
                MainActivity mainActivity = (MainActivity) activity;
                NavController navController = Navigation.findNavController(mainActivity, R.id.nav_host);
                navController.navigate(MainDirections.login().setFeature(feature));
            });
        } else {
            Bundle args = new LoginFragmentArgs.Builder().setFeature(feature).build().toBundle();

            PendingIntent pendingIntent = new NavDeepLinkBuilder(application)
                    .setGraph(R.navigation.main)
                    .setDestination(R.id.nav_login)
                    .setArguments(args)
                    .setComponentName(MainActivity.class)
                    .createPendingIntent();

            try {
                pendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                Log.e(LOG_TAG, "Could not launch login fragment.", e);
                throw e;
            }
        }
    }

    private static Pair<String, char[]> loadUsernameAndPassword() throws InvalidCredentialsException {
        Pair<String, char[]> out = PasswordStorage.loadUsernameAndPassword();

        if (out.first == null || out.second == null) {
            throw new InvalidCredentialsException("Could not obtain credentials.");
        }

        return out;
    }
}
