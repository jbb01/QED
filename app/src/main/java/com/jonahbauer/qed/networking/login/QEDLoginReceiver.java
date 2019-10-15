package com.jonahbauer.qed.networking.login;

public interface QEDLoginReceiver {
    void onLoginSuccess();
    void onLoginError();
    void onLoginNetworkError();
}
