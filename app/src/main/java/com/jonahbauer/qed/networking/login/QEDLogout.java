package com.jonahbauer.qed.networking.login;

import android.os.AsyncTask;

import com.jonahbauer.qed.crypt.PasswordStorage;
import com.jonahbauer.qed.networking.NetworkConstants;
import com.jonahbauer.qed.networking.cookies.QEDCookieHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import javax.net.ssl.HttpsURLConnection;

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
        boolean out = true;
        out &= logoutChat();
        out &= logoutDatabase();
        out &= logoutGallery();

        PasswordStorage.clearCredentials();
        QEDCookieHandler.invalidate();

        return out;
    }

    public static CompletableFuture<Boolean> logoutAsync() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> future.complete(logout()));
        return future;
    }
}
