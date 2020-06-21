package com.jonahbauer.qed.networking;

import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;

import com.jonahbauer.qed.Application;

@SuppressWarnings("unused")
public interface QEDPageStreamReceiver {
    String REASON_NETWORK = "network error";
    String REASON_UNABLE_TO_LOG_IN = "unable to log in";

    void onPageReceived(String tag);

    @CallSuper
    default void onError(String tag, @Nullable String reason, @Nullable Throwable cause) {
        Log.e(Application.LOG_TAG_ERROR, tag + ": " + reason, cause);
    }

    void onProgressUpdate(String tag, long done, long total);
}