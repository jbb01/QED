package com.jonahbauer.qed.qedgallery;

import java.util.List;

@Deprecated
public interface QEDGalleryListReceiver {
    void onAlbumListReceived(List<Album> galleries);
}
