package com.jonahbauer.qed.networking.async;

import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.util.Callback;

@SuppressWarnings("unused")
public interface QEDPageReceiver<T> extends Callback<T> {
    @MainThread
    void onResult(@NonNull T out);

    @CallSuper
    @MainThread
    default void onError(T out, @NonNull Reason reason, @Nullable Throwable cause) {
        Log.e(getClass().getName(), out + ":" + reason, cause);
    }

    @MainThread
    default void onError(T out, @Nullable Throwable cause) {
        onError(out, Reason.guess(cause), cause);
    }
}
