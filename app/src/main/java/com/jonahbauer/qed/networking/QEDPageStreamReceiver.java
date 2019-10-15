package com.jonahbauer.qed.networking;

public interface QEDPageStreamReceiver {
    void onPageReceived(String tag);
    void onNetworkError(String tag);
    void onStreamError(String tag);
    void onProgressUpdate(String tag, long done, long total);
}
