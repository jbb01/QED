package com.jonahbauer.qed.networking;

import android.util.Log;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.model.Message;

public interface ChatWebSocketListener {
    String REASON_NETWORK = "network error";
    String REASON_INVALID_CREDENTIALS = "invalid credentials";

    void onMessage(@NonNull Message message);

    default void onError(String reason, Throwable cause) {
        Log.e(getClass().getName(), reason != null ? reason : "", cause);
    }
}
