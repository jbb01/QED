package com.jonahbauer.qed.networking.async;

import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.networking.Feature;
import com.jonahbauer.qed.networking.NetworkUtils;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.networking.exceptions.InvalidCredentialsException;
import com.jonahbauer.qed.networking.login.QEDLogin;
import com.jonahbauer.qed.networking.parser.Parser;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;
import java.util.function.BiFunction;

import javax.net.ssl.HttpsURLConnection;

/**
 * Asynchronously loads a web page from the qed servers.
 *
 * @param <T> callback return value type
 */
public final class AsyncLoadQEDPage<T> extends AsyncTask<Void, Void, String> {
    private static final String LOG_TAG = AsyncLoadQEDPage.class.getName();

    private final Feature mFeature;
    private final String mUrl;
    private final QEDPageReceiver<T> mReceiver;
    private final BiFunction<T, String, T> mParser;
    private final T mHolder;

    private Exception mException;

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
        Log.d(LOG_TAG, "Loading Page " + url + " with feature " + feature);
        this.mFeature = feature;
        this.mUrl = url;
        this.mReceiver = receiver;
        this.mHolder = holder;
        this.mParser = parser;
    }

    @Override
    protected String doInBackground(Void... voids) {
        try {
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
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            mException = e;
            return null;
        }
    }

    @Override
    protected void onPostExecute(String s) {
        if (s != null) {
            T out;

            try {
                out = mParser.apply(mHolder, s);
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                mReceiver.onError(mHolder, Reason.guess(mException), null);
                return;
            }

            mReceiver.onPageReceived(out);
        } else if (mException != null) {
            mReceiver.onError(mHolder, Reason.guess(mException), mException);
        }
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
