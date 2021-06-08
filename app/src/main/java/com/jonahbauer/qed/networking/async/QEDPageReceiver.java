package com.jonahbauer.qed.networking.async;

import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.networking.Reason;

@SuppressWarnings("unused")
public interface QEDPageReceiver<T> {
    @MainThread
    void onPageReceived(@NonNull T out);

    @CallSuper
    @MainThread
    default void onError(T out, @Reason String reason, @Nullable Throwable cause) {
        Log.e(Application.LOG_TAG_ERROR, out + ":" + reason, cause);
    }
}
