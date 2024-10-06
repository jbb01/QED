package eu.jonahbauer.qed.networking.async;

import android.util.Log;
import androidx.annotation.CallSuper;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import eu.jonahbauer.qed.networking.Reason;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public interface QEDPageReceiver<T> {
    static <T> QEDPageReceiver<T> fromFuture(CompletableFuture<T> future) {
        return new QEDPageReceiver<>() {
            @Override
            public void onResult(@NonNull T out) {
                future.complete(out);
            }

            @Override
            public void onError(T out, @NonNull Reason reason, @Nullable Throwable cause) {
                QEDPageReceiver.super.onError(out, reason, cause);
                future.completeExceptionally(cause == null ? new Exception() : cause);
            }
        };
    }

    @MainThread
    void onResult(@NonNull T out);

    @CallSuper
    @MainThread
    default void onError(T out, @NonNull Reason reason, @Nullable Throwable cause) {
        Log.e(getClass().getName(), "An error occurred. reason=" + reason + " data=" + out, cause);
    }

    @MainThread
    default void onError(T out, @Nullable Throwable cause) {
        onError(out, Reason.guess(cause), cause);
    }
}
