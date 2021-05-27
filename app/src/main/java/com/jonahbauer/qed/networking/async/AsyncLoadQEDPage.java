package com.jonahbauer.qed.networking.async;

import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.networking.Feature;
import com.jonahbauer.qed.networking.NetworkUtils;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.networking.exceptions.InvalidCredentialsException;
import com.jonahbauer.qed.networking.login.QEDLogin;
import com.jonahbauer.qed.networking.parser.Parser;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.BiFunction;

import javax.net.ssl.HttpsURLConnection;

/**
 * Asynchronously loads a web page from the qed servers.
 *
 * @param <T> callback return value type
 */
public final class AsyncLoadQEDPage<T> extends AsyncTask<Void, Void, String> {
    private final Feature mFeature;
    private final Application mApplication;
    private final String mUrl;
    private final QEDPageReceiver<T> mReceiver;
    private final BiFunction<T, String, T> mParser;
    private final T mHolder;

    /**
     * Loads the website from url.
     *
     * @param feature the type of the qed website to be loaded, used to choose correct form of authentication
     * @param url the url of the qed website
     * @param receiver a receiver that will receive the relevant data collected by {@code postExecute} or an error message
     * @param parser a function to collect relevant data from the html string, called on ui thread
     */
    public AsyncLoadQEDPage(@NonNull Feature feature,
                     @NonNull String url,
                     @NonNull T holder,
                     @NonNull Parser<T> parser,
                     @NonNull QEDPageReceiver<T> receiver) {
        this.mFeature = feature;
        this.mUrl = url;
        this.mReceiver = receiver;
        this.mHolder = holder;
        this.mParser = parser;

        SoftReference<Application> applicationReference = Application.getApplicationReference();
        mApplication = applicationReference.get();
    }

    @Override
    protected String doInBackground(Void... voids) {
        if (mApplication == null) {
            Log.e(Application.LOG_TAG_ERROR, "", new Exception("Application is null!"));
            return null;
        }

        try {
            // try to connect
            HttpsURLConnection httpsURLConnection = createConnection();
            httpsURLConnection.connect();

            // check for login error
            if (isLoginError(mFeature, httpsURLConnection)) {
                httpsURLConnection.disconnect();

                // login
                QEDLogin.login(mFeature);

                // retry connection
                httpsURLConnection = createConnection();
                httpsURLConnection.connect();

                // check for login error once more
                // if authentication failed after successful login -> throw exception
                if (isLoginError(mFeature, httpsURLConnection)) {
                    throw new InvalidCredentialsException(new AssertionError("request not authenticated after login"));
                }
            }

            String out = NetworkUtils.readPage(httpsURLConnection);
            httpsURLConnection.disconnect();

            return out;
        } catch (IOException e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
            mReceiver.onError(mHolder, Reason.NETWORK, e);
            return null;
        } catch (InvalidCredentialsException e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
            mReceiver.onError(mHolder, Reason.UNABLE_TO_LOG_IN, null);
            return null;
        } catch (Exception e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
            mReceiver.onError(mHolder, Reason.UNKNOWN, null);
            return null;
        }
    }

    @Override
    protected void onPostExecute(String s) {
        if (s != null)
            mReceiver.onPageReceived(mParser.apply(mHolder, s));
    }

    @NotNull
    private HttpsURLConnection createConnection() throws IOException {
        HttpsURLConnection httpsURLConnection = (HttpsURLConnection) (new URL(mUrl).openConnection());
        httpsURLConnection.setRequestMethod("GET");
        httpsURLConnection.setInstanceFollowRedirects(false);
        httpsURLConnection.setUseCaches(false);

        return httpsURLConnection;
    }

    private boolean isLoginError(Feature feature, HttpURLConnection connection) {
        String location = connection.getHeaderField("Location");

        switch (feature) {
            case CHAT:
            case GALLERY:
                return location != null && location.startsWith("account");
            case DATABASE:
                return location != null && location.contains("login");
            default:
                throw new IllegalArgumentException("unknown feature " + feature);
        }
    }
}
