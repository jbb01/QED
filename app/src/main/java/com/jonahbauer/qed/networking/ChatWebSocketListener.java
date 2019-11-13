package com.jonahbauer.qed.networking;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.chat.Message;

public interface ChatWebSocketListener {
    void onMessage(@NonNull Message message);

    void onNetworkError();
    void onUnknownError();
}
