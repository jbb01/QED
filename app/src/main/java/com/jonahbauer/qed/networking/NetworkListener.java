package com.jonahbauer.qed.networking;

public interface NetworkListener {
    void onConnectionFail();
    void onConnectionRegain();
}
