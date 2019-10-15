package com.jonahbauer.qed.qedgallery;

@Deprecated
public interface QEDGalleryImageReceiver {
    void onGalleryImageReceived(Image image, boolean success);
    void onGalleryImageProgressUpdate(long done, long total);
}
