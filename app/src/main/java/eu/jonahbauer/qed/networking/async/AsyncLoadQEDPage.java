package eu.jonahbauer.qed.networking.async;

import androidx.annotation.NonNull;

import eu.jonahbauer.qed.networking.Feature;
import eu.jonahbauer.qed.networking.NetworkUtils;
import eu.jonahbauer.qed.networking.exceptions.InvalidCredentialsException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.Callable;

import javax.net.ssl.HttpsURLConnection;

public final class AsyncLoadQEDPage extends BaseAsyncLoadQEDPage implements Callable<String> {

    private final Feature mFeature;
    private final String mUrl;

    public AsyncLoadQEDPage(@NonNull Feature mFeature,
                            @NonNull String mUrl) {
        this.mFeature = mFeature;
        this.mUrl = mUrl;
    }

    @Override
    public String call() throws IOException, InvalidCredentialsException {
        HttpsURLConnection httpsURLConnection = connectAndLogin(mUrl, mFeature);
        if (httpsURLConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException("Status Code is not 200.");
        }

        String out = NetworkUtils.readPage(httpsURLConnection);
        httpsURLConnection.disconnect();

        return out;
    }
}
