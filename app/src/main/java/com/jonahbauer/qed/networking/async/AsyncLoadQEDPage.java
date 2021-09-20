package com.jonahbauer.qed.networking.async;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.networking.Feature;
import com.jonahbauer.qed.networking.NetworkUtils;
import com.jonahbauer.qed.networking.exceptions.InvalidCredentialsException;
import com.jonahbauer.qed.networking.login.QEDLogin;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Callable;

import javax.net.ssl.HttpsURLConnection;

public final class AsyncLoadQEDPage implements Callable<String> {

    private final Feature mFeature;
    private final String mUrl;

    public AsyncLoadQEDPage(@NonNull Feature mFeature,
                            @NonNull String mUrl) {
        this.mFeature = mFeature;
        this.mUrl = mUrl;
    }

    @Override
    public String call() throws IOException, InvalidCredentialsException {
        // try to connect
        HttpsURLConnection httpsURLConnection = createConnection();
        httpsURLConnection.connect();

        // check for login error
        if (NetworkUtils.isLoginError(mFeature, httpsURLConnection)) {
            httpsURLConnection.disconnect();

            // login
            QEDLogin.login(mFeature);

            // retry connection
            httpsURLConnection = createConnection();
            httpsURLConnection.connect();

            // check for login error once more
            // if authentication failed after successful login -> throw exception
            if (NetworkUtils.isLoginError(mFeature, httpsURLConnection)) {
                throw new InvalidCredentialsException(new AssertionError("request not authenticated after login"));
            }
        }

        String out = NetworkUtils.readPage(httpsURLConnection);
        httpsURLConnection.disconnect();

        return out;
    }

    @NotNull
    private HttpsURLConnection createConnection() throws IOException {
        HttpsURLConnection httpsURLConnection = (HttpsURLConnection) (new URL(mUrl).openConnection());
        httpsURLConnection.setRequestMethod("GET");
        httpsURLConnection.setInstanceFollowRedirects(false);
        httpsURLConnection.setUseCaches(false);

        return httpsURLConnection;
    }
}
