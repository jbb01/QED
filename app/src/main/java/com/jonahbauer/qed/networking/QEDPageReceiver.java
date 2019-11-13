package com.jonahbauer.qed.networking;

import androidx.annotation.Nullable;

public interface QEDPageReceiver<T> {
    void onPageReceived(String tag, @Nullable T out);
    void onNetworkError(String tag);
}
