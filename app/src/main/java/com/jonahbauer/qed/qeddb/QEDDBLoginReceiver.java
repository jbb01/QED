package com.jonahbauer.qed.qeddb;

public interface QEDDBLoginReceiver {
    void onReceiveSessionId(char[] sessionId, char[] cookie);
}
