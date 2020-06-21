package com.jonahbauer.qed.networking;

import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.networking.login.InvalidCredentialsException;
import com.jonahbauer.qed.networking.login.QEDLogin;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.net.ssl.HttpsURLConnection;

/**
 * Asynchronously loads a web page from the qed servers.
 *
 * @param <T> callback return value type
 */
public class AsyncLoadQEDPage<T> extends AsyncTask<Void, Void, String> {
    private Feature mFeature;
    private Application mApplication;
    private String mUrl;
    private String mTag;
    private QEDPageReceiver<T> mReceiver;
    private Map<String,String> mCookies;
    private Function<String, T> mPostExecute;

    /**
     * Loads the website from url. url might contain the wildcards "{\@userid}", "{\@pwhash}", "{\@sessionid}", "{\@sessionid2}"
     *
     * @param feature the type of the qed website to be loaded, used to choose correct form of authentication
     * @param url the url of the qed website
     * @param receiver a receiver that will receive the relevant data collected by {@code postExecute} or an error message
     * @param tag a string tag used when calling receiver methods
     * @param postExecute a function to collect relevant data from the html string, called on ui thread
     * @param cookies a map of cookies, the cookies required for authenticating to the server are automatically added depending on {@code feature}
     */
    AsyncLoadQEDPage(@NonNull Feature feature, @NonNull String url, @NonNull QEDPageReceiver<T> receiver, String tag, @NonNull Function<String, T> postExecute, @Nullable Map<String, String> cookies) {
        this.mFeature = feature;
        this.mUrl = url;
        this.mTag = tag;
        this.mReceiver = receiver;
        this.mPostExecute = postExecute;

        this.mCookies = cookies;
        if (cookies == null) this.mCookies = new HashMap<>();

        SoftReference<Application> applicationReference = Application.getApplicationReference();
        mApplication = applicationReference.get();

        QEDLogin.addCookies(feature, this.mCookies);
    }

    @Override
    protected String doInBackground(Void... voids) {
        if (mApplication == null) {
            Log.e(Application.LOG_TAG_ERROR, "", new Exception("Application is null!"));
            return null;
        }

        try {
            QEDLogin.ensureDataAvailable(mFeature);

            HttpsURLConnection httpsURLConnection = createConnection();
            httpsURLConnection.connect();

            String out = null;
            String location = httpsURLConnection.getHeaderField("Location");
            boolean loginError = false;
            if (mFeature == Feature.CHAT || mFeature == Feature.GALLERY) loginError = location != null && location.startsWith("account");
            else if (mFeature == Feature.DATABASE) {
                out = NetworkUtils.readPage(httpsURLConnection);
                loginError = out.contains("nicht eingeloggt");
            }
            if (loginError) {
                httpsURLConnection.disconnect();

                QEDLogin.login(mFeature);

                httpsURLConnection = createConnection();
                httpsURLConnection.connect();

                location = httpsURLConnection.getHeaderField("Location");
                loginError = false;
                if (mFeature == Feature.CHAT || mFeature == Feature.GALLERY) loginError = location != null && location.startsWith("account");
                else if (mFeature == Feature.DATABASE) {
                    out = NetworkUtils.readPage(httpsURLConnection);
                    loginError = out.contains("nicht eingeloggt");
                }
                if (loginError)
                    throw new InvalidCredentialsException(null);

                if (mFeature != Feature.DATABASE) out = NetworkUtils.readPage(httpsURLConnection);
                httpsURLConnection.disconnect();

                return out;
            }

            if (mFeature != Feature.DATABASE) out = NetworkUtils.readPage(httpsURLConnection);
            httpsURLConnection.disconnect();

            return out;
        } catch (IOException | NoNetworkException e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
            mReceiver.onError(mTag, QEDPageReceiver.REASON_NETWORK, e);
            return null;
        } catch (InvalidCredentialsException e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
            mReceiver.onError(mTag, QEDPageReceiver.REASON_UNABLE_TO_LOG_IN, null);
            return null;
        }
    }

    @Override
    protected void onPostExecute(String s) {
        if (s != null)
            mReceiver.onPageReceived(mTag, mPostExecute.apply(s));
    }

    @NotNull
    private HttpsURLConnection createConnection() throws IOException {
        HttpsURLConnection httpsURLConnection = (HttpsURLConnection) (new URL(QEDLogin.formatString(mFeature, mUrl)).openConnection());
        httpsURLConnection.setRequestMethod("POST");
        httpsURLConnection.setInstanceFollowRedirects(false);
        httpsURLConnection.setUseCaches(false);

        String cookie = QEDLogin.loadCookies(mFeature, mCookies);
        httpsURLConnection.setRequestProperty("Cookie", cookie);

        return httpsURLConnection;
    }
}
