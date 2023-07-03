package eu.jonahbauer.qed.networking.async;

import androidx.annotation.NonNull;

import androidx.annotation.Nullable;
import eu.jonahbauer.qed.networking.Feature;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.function.Consumer;

import javax.net.ssl.HttpsURLConnection;

import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import it.unimi.dsi.fastutil.longs.LongLongPair;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class AsyncLoadQEDPageToStream extends BaseAsyncLoadQEDPage implements ObservableOnSubscribe<LongLongPair> {

    private final @NonNull Feature mFeature;
    private final @NonNull String mUrl;
    private final @NonNull OutputStream mOutputStream;
    private final @Nullable Consumer<HttpsURLConnection> processor;

    @Override
    public void subscribe(@NonNull ObservableEmitter<LongLongPair> emitter) throws Throwable {
        HttpsURLConnection httpsURLConnection = connectAndLogin(mUrl, mFeature);
        if (httpsURLConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            emitter.onError(new IOException("Status Code is not 200."));
            return;
        }

        if (processor != null && !emitter.isDisposed()) {
            processor.accept(httpsURLConnection);
        }

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
