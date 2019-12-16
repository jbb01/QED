package com.jonahbauer.qed.networking;

import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;

import com.jonahbauer.qed.Application;

public interface QEDPageReceiver<T> {
    String REASON_UNKNOWN = "unknown error";
    String REASON_NETWORK = "network error";
    String REASON_UNABLE_TO_LOG_IN = "unable to log in";

    void onPageReceived(String tag, @Nullable T out);

    @CallSuper
    default void onError(String tag, @Nullable String reason, @Nullable Throwable cause) {
        Log.e(Application.LOG_TAG_ERROR, tag + ": " + reason, cause);
    }
}
