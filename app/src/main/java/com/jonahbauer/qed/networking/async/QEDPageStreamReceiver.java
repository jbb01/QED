package com.jonahbauer.qed.networking.async;

@SuppressWarnings("unused")
public interface QEDPageStreamReceiver<T> extends QEDPageReceiver<T> {
    void onProgressUpdate(T obj, long done, long total);
}