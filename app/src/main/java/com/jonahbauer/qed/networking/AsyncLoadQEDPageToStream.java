package com.jonahbauer.qed.networking;

import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.networking.login.InvalidCredentialsException;
import com.jonahbauer.qed.networking.login.QEDLogin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class AsyncLoadQEDPageToStream extends AsyncTask<Void, Long, Boolean> {
    private InputStream mIn;

    private final Feature mFeature;
    private final String mUrl;
    private final QEDPageStreamReceiver mReceiver;
    private final String mTag;
    private final OutputStream mOutputStream;

    private final Map<String,String> mCookies;


    /**
     * Loads the website from url to the provided {@code OutputStream}. The url might contain the wildcards "{\@userid}", "{\@pwhash}", "{\@sessionid}", "{\@sessionid2}"
     *
     * @param feature the type of the qed website to be loaded, used to choose correct form of authentication
     * @param url the url of the qed website
     * @param receiver a receiver that will receive the relevant data collected by {@code postExecute} or an error message
     * @param tag a string tag used when calling receiver methods
     * @param outputStream the target to which the page will be forwarded
     */
    AsyncLoadQEDPageToStream(@NonNull Feature feature, @NonNull String url, @NonNull QEDPageStreamReceiver receiver, String tag, @NonNull OutputStream outputStream) {
        if (feature == Feature.DATABASE) throw new Feature.FeatureNotSupportedException();

        this.mFeature = feature;
        this.mUrl = url;
        this.mReceiver = receiver;
        this.mTag = tag;
        this.mOutputStream = outputStream;

        this.mCookies = new HashMap<>();
        QEDLogin.addCookies(feature, mCookies);
    }


    @Override
    protected Boolean doInBackground(Void... voids) {
        try {
            QEDLogin.ensureDataAvailable(mFeature);

            HttpsURLConnection httpsURLConnection = createConnection();
            if (isCancelled()) return false;
            httpsURLConnection.connect();

            String location = httpsURLConnection.getHeaderField("Location");
            boolean loginError = location != null && location.startsWith("account");
            if (loginError) {
                httpsURLConnection.disconnect();

                QEDLogin.login(mFeature);
                httpsURLConnection = createConnection();
                if (isCancelled()) return false;
                httpsURLConnection.connect();

                location = httpsURLConnection.getHeaderField("Location");
                loginError = location != null && location.startsWith("account");
                if (loginError)
                    throw new InvalidCredentialsException(null);
            }

            mIn = httpsURLConnection.getInputStream();
            try {
                copyStream(mIn, mOutputStream, httpsURLConnection.getContentLength());
            } catch (IOException e) {
                mReceiver.onError(mTag, null, e);
                return false;
            }
            mIn.close();
            mOutputStream.close();

            httpsURLConnection.disconnect();

            return true;
        } catch (IOException | NoNetworkException e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
            mReceiver.onError(mTag, QEDPageReceiver.REASON_NETWORK, e);
            return false;
        } catch (InvalidCredentialsException e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
            mReceiver.onError(mTag, QEDPageReceiver.REASON_UNABLE_TO_LOG_IN, e);
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean b) {
        if (b != null && b)
            mReceiver.onPageReceived(mTag);
    }

    @Override
    protected void onProgressUpdate(Long... values) {
        mReceiver.onProgressUpdate(mTag, values[0], values[1]);
    }

    @NonNull
    private HttpsURLConnection createConnection() throws IOException {
        HttpsURLConnection httpsURLConnection = (HttpsURLConnection) (new URL(QEDLogin.formatString(mFeature, mUrl)).openConnection());
        httpsURLConnection.setRequestMethod("GET");
        httpsURLConnection.setDoInput(true);
        httpsURLConnection.setInstanceFollowRedirects(false);
        httpsURLConnection.setUseCaches(false);
        httpsURLConnection.setRequestProperty("Cookie", QEDLogin.loadCookies(mFeature, mCookies));

        return httpsURLConnection;
    }

    private void copyStream(@NonNull InputStream in, @NonNull OutputStream out, long contentLength) throws IOException {
        byte[] buffer = new byte[4 * 1024]; // 4 kilobyte
        long count = 0;
        int n;
        int i = 0;
        while (-1 != (n = in.read(buffer)) && !isCancelled()) {
            out.write(buffer, 0, n);
            count += n;
            i++;
            if (i == 64) { // 64*4 = 256 kilobyte
                i = 0;
                publishProgress(count, contentLength);
            }
        }
    }

    @Override
    protected void onCancelled() {
        try {
            if (mOutputStream != null)
                mOutputStream.close();
            if (mIn != null)
                mIn.close();
        } catch (IOException e) {
            Log.e(Application.LOG_TAG_ERROR, e.getMessage(), e);
        }
    }
}
