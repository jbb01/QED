package com.jonahbauer.qed.qedgallery;

import android.graphics.Bitmap;

public interface QEDGalleryThumbnailReceiver {
    void onGalleryThumbnailReceived(Image image, Bitmap bitmap);
}
