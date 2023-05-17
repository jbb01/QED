package eu.jonahbauer.qed.networking.async;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;

import eu.jonahbauer.qed.networking.Feature;
import eu.jonahbauer.qed.networking.NetworkUtils;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Callable;

import javax.net.ssl.HttpsURLConnection;

public final class AsyncLoadQEDPageToImage extends BaseAsyncLoadQEDPage implements Callable<Optional<Bitmap>> {

    private final Feature mFeature;
    private final String mUrl;

    public AsyncLoadQEDPageToImage(@NonNull Feature mFeature,
                                   @NonNull String mUrl) {
        this.mFeature = mFeature;
        this.mUrl = mUrl;
    }

    @Override
    public Optional<Bitmap> call() throws Exception {
        HttpsURLConnection httpsURLConnection = connectAndLogin(mUrl, mFeature);

        // thumbnails for image and video get redirected
        if (httpsURLConnection.getResponseCode() == 302) {
            return Optional.empty();
        }

        byte[] data = NetworkUtils.readAllBytes(httpsURLConnection.getInputStream());

        httpsURLConnection.disconnect();

        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);


        if (bitmap == null) {
            throw new IOException("Could not load image from " + mUrl + ".");
        } else {
            return Optional.of(bitmap);
        }
    }
}
