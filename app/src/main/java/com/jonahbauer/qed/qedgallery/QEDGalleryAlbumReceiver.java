package com.jonahbauer.qed.qedgallery;

import java.util.List;

public interface QEDGalleryAlbumReceiver {
    void onGalleryAlbumReceived(List<Image> images);
}
