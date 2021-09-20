package com.jonahbauer.qed.networking.login;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.jonahbauer.qed.crypt.PasswordStorage;
import com.jonahbauer.qed.networking.NetworkConstants;
import com.jonahbauer.qed.networking.cookies.QEDCookieHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import javax.net.ssl.HttpsURLConnection;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.experimental.UtilityClass;

@UtilityClass
public class QEDLogout {
    private static boolean logoutChat() {
        try {
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) new URL(NetworkConstants.CHAT_SERVER_LOGOUT).openConnection();
            httpsURLConnection.setRequestMethod("POST");
            httpsURLConnection.setDoOutput(true);
            httpsURLConnection.setInstanceFollowRedirects(false);
            httpsURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpsURLConnection.setRequestProperty("Content-Encoding", "utf-8");

            try (OutputStream os = httpsURLConnection.getOutputStream()) {
                os.write("logout=1".getBytes(StandardCharsets.UTF_8));
            }

            httpsURLConnection.connect();
            httpsURLConnection.getResponseCode();

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean logoutDatabase() {
        try {
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) new URL(NetworkConstants.DATABASE_SERVER_LOGOUT).openConnection();
            httpsURLConnection.setRequestMethod("GET");
            httpsURLConnection.setInstanceFollowRedirects(false);
            httpsURLConnection.connect();
            httpsURLConnection.getResponseCode();

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean logoutGallery() {
        try {
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) new URL(NetworkConstants.GALLERY_SERVER_LOGOUT).openConnection();
            httpsURLConnection.setRequestMethod("GET");
            httpsURLConnection.setInstanceFollowRedirects(false);
            httpsURLConnection.connect();
            httpsURLConnection.getResponseCode();

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean logout() {
        try {
            return logoutChat() & logoutDatabase() & logoutGallery();
        } finally {
            PasswordStorage.clearCredentials();
            QEDCookieHandler.invalidate();
        }
    }

    @NonNull
    @SuppressWarnings("UnusedReturnValue")
    public static Disposable logoutAsync(@MainThread Consumer<Boolean> callback) {
        return Single.fromCallable(QEDLogout::logout)
                     .subscribeOn(Schedulers.io())
                     .observeOn(AndroidSchedulers.mainThread())
                     .subscribe(
                             callback::accept,
                             err -> callback.accept(false)
                     );
    }
}
