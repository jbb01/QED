package com.jonahbauer.qed.networking.async;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.networking.Feature;

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
            String location = httpsURLConnection.getHeaderField("Location");
            if (location != null) {
                if (location.contains("video-x-generic.png")) {
                    return Optional.empty();
                } else if (location.contains("audio-x-generic.png")) {
                    return Optional.empty();
                }
            }
        }

        Bitmap bitmap = BitmapFactory.decodeStream(httpsURLConnection.getInputStream());

        httpsURLConnection.disconnect();

        //noinspection OptionalAssignedToNull
        return bitmap != null ? Optional.of(bitmap) : null;
    }
}
