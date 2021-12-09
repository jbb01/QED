package com.jonahbauer.qed.networking.async;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.networking.Feature;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.net.ssl.HttpsURLConnection;

import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import it.unimi.dsi.fastutil.longs.LongLongPair;

public final class AsyncLoadQEDPageToStream extends BaseAsyncLoadQEDPage implements ObservableOnSubscribe<LongLongPair> {

    private final Feature mFeature;
    private final String mUrl;
    private final OutputStream mOutputStream;

    public AsyncLoadQEDPageToStream(@NonNull Feature mFeature,
                                    @NonNull String mUrl,
                                    @NonNull OutputStream mOutputStream) {
        this.mFeature = mFeature;
        this.mUrl = mUrl;
        this.mOutputStream = mOutputStream;
    }

    @Override
    public void subscribe(@NonNull ObservableEmitter<LongLongPair> emitter) throws Throwable {
        HttpsURLConnection httpsURLConnection = connectAndLogin(mUrl, mFeature);

        // copy input stream to output stream
        try (InputStream inputStream = httpsURLConnection.getInputStream(); mOutputStream) {
            copyStream(inputStream, mOutputStream, httpsURLConnection.getContentLength(), emitter);
        }

        httpsURLConnection.disconnect();

        emitter.onComplete();
    }

    private void copyStream(@NonNull InputStream in,
                            @NonNull OutputStream out,
                            long contentLength,
                            ObservableEmitter<LongLongPair> emitter) throws IOException {
        byte[] buffer = new byte[4 * 1024]; // 4 kilobyte
        long count = 0;
        int n;
        int i = 0;
        while (-1 != (n = in.read(buffer)) && !emitter.isDisposed()) {
            out.write(buffer, 0, n);
            count += n;
            i++;
            if (i == 64) { // 64*4 = 256 kilobyte
                i = 0;
                emitter.onNext(LongLongPair.of(count, contentLength));
            }
        }
        emitter.onNext(LongLongPair.of(count, contentLength));
    }
}
