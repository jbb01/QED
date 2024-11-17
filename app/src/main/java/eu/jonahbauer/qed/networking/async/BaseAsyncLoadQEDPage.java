package eu.jonahbauer.qed.networking.async;

import androidx.annotation.NonNull;

import eu.jonahbauer.qed.networking.Feature;
import eu.jonahbauer.qed.network.util.NetworkUtil;
import eu.jonahbauer.qed.networking.cookies.QEDCookieHandler;
import eu.jonahbauer.qed.networking.exceptions.InvalidCredentialsException;
import eu.jonahbauer.qed.networking.login.QEDLogin;

import java.io.IOException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

abstract class BaseAsyncLoadQEDPage {

    protected HttpsURLConnection connectAndLogin(String url, Feature feature) throws IOException, InvalidCredentialsException {
        // try to connect
        HttpsURLConnection httpsURLConnection = createConnection(url);
        httpsURLConnection.connect();

        // check for login error
        if (NetworkUtil.isLoginError(feature, httpsURLConnection)) {
            httpsURLConnection.disconnect();

            // login
            QEDLogin.login(feature);

            // retry connection
            httpsURLConnection = createConnection(url);
            httpsURLConnection.connect();

            // check for login error once more
            // if authentication failed after successful login -> throw exception
            if (NetworkUtil.isLoginError(feature, httpsURLConnection)) {
                throw new InvalidCredentialsException(new AssertionError("request not authenticated after login"));
            }
        }

        return httpsURLConnection;
    }

    @NonNull
    private HttpsURLConnection createConnection(String url) throws IOException {
        QEDCookieHandler.await();
        HttpsURLConnection httpsURLConnection = (HttpsURLConnection) (new URL(url).openConnection());
        httpsURLConnection.setRequestMethod("GET");
        httpsURLConnection.setInstanceFollowRedirects(false);
        httpsURLConnection.setUseCaches(false);

        return httpsURLConnection;
    }
}
