package com.jonahbauer.qed.qedgallery;

@Deprecated
public interface QEDGalleryLoginReceiver {
    void onLoginFinish(boolean success);
    void onLoginError();
}
