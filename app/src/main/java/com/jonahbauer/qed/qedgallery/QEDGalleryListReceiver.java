package com.jonahbauer.qed.qedgallery;

import java.util.List;

public interface QEDGalleryListReceiver {
    void onGalleryListReceived(List<Gallery> galleries);
}
