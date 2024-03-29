package eu.jonahbauer.qed.networking.async;

import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import eu.jonahbauer.qed.networking.Reason;

@SuppressWarnings("unused")
public interface QEDPageReceiver<T> {
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
