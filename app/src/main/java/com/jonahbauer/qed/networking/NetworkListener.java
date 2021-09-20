package com.jonahbauer.qed.networking;

import androidx.annotation.MainThread;

public interface NetworkListener {
    @MainThread
    void onConnectionFail();

    @MainThread
    void onConnectionRegain();
}
