package com.jonahbauer.qed.qedgallery;

public interface QEDGalleryLoginReceiver {
    void onReceiveSessionId(char[] sessionId, char[] pwhash, char[] userid);
}
