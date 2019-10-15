package com.jonahbauer.qed.networking;

public interface QEDPageReceiver<T> {
    void onPageReceived(String tag, T out);
    void onNetworkError(String tag);
}
