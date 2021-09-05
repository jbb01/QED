package com.jonahbauer.qed.networking.async;

import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.networking.Feature;
import com.jonahbauer.qed.networking.NetworkUtils;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.networking.exceptions.InvalidCredentialsException;
import com.jonahbauer.qed.networking.login.QEDLogin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public final class AsyncLoadQEDPageToStream<T> extends AsyncTask<Void, Long, Boolean> {
    private static final String LOG_TAG = AsyncLoadQEDPageToStream.class.getName();

    private InputStream mIn;

    private final Feature mFeature;
    private final String mUrl;
    private final QEDPageStreamReceiver<T> mReceiver;
    private final OutputStream mOutputStream;
    private final T mHolder;

    private Exception mException;


    /**
     * Loads the website from url to the provided {@code OutputStream}.
     *
     * @param feature the type of the qed website to be loaded, used to choose correct form of authentication
     * @param url the url of the qed website
     * @param receiver a receiver that will receive the relevant data collected by {@code postExecute} or an error message
     * @param outputStream the target to which the page will be forwarded
     */
    public AsyncLoadQEDPageToStream(@NonNull Feature feature,
                             @NonNull String url,
                             @NonNull T holder,
                             @NonNull QEDPageStreamReceiver<T> receiver,
                             @NonNull OutputStream outputStream) {
        Log.d(LOG_TAG, "Loading Page " + url + " with feature " + feature);
        if (feature == Feature.DATABASE) throw new Feature.FeatureNotSupportedException();

        this.mFeature = feature;
        this.mUrl = url;
        this.mReceiver = receiver;
        this.mOutputStream = outputStream;
        this.mHolder = holder;
    }


    @Override
    protected Boolean doInBackground(Void... voids) {
        try {
            // try to connect
            HttpsURLConnection httpsURLConnection = createConnection();
            if (isCancelled()) return false;
            httpsURLConnection.connect();

            // check for login error
            if (NetworkUtils.isLoginError(mFeature, httpsURLConnection)) {
                httpsURLConnection.disconnect();

                // login
                QEDLogin.login(mFeature);

                // retry connection
                httpsURLConnection = createConnection();
                if (isCancelled()) return false;
                httpsURLConnection.connect();

                // check for login error once more
                // if authentication failed after successful login -> throw exception
                if (NetworkUtils.isLoginError(mFeature, httpsURLConnection)) {
                    throw new InvalidCredentialsException(new AssertionError("request not authenticated after login"));
                }
            }

            // copy input stream to output stream
            mIn = httpsURLConnection.getInputStream();
            try {
                copyStream(mIn, mOutputStream, httpsURLConnection.getContentLength());
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                mException = e;
                return null;
            }
            mIn.close();
            mOutputStream.close();

            httpsURLConnection.disconnect();

            return true;
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            mException = e;
            return null;
        }
    }

    @Override
    protected void onPostExecute(Boolean b) {
        if (b != null && b) {
            mReceiver.onPageReceived(mHolder);
        } else if (mException != null) {
            mReceiver.onError(mHolder, Reason.guess(mException), mException);
        }
    }

    @Override
    protected void onProgressUpdate(Long... values) {
        mReceiver.onProgressUpdate(mHolder, values[0], values[1]);
    }

    @NonNull
    private HttpsURLConnection createConnection() throws IOException {
        HttpsURLConnection httpsURLConnection = (HttpsURLConnection) (new URL(mUrl).openConnection());
        httpsURLConnection.setRequestMethod("GET");
        httpsURLConnection.setDoInput(true);
        httpsURLConnection.setInstanceFollowRedirects(false);
        httpsURLConnection.setUseCaches(false);

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
        publishProgress(count, contentLength);
    }

    @Override
    protected void onCancelled() {
        try {
            if (mOutputStream != null)
                mOutputStream.close();
            if (mIn != null)
                mIn.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
    }
}
