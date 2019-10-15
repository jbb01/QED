package com.jonahbauer.qed.qeddb;

/**
 * @deprecated
 *
 * replaced with {@link com.jonahbauer.qed.networking.login.QEDLoginReceiver}
 */
@Deprecated
public interface QEDDBLoginReceiver {
    void onLoginFinish(boolean success);
}
