package com.jonahbauer.qed.networking.async;

import androidx.annotation.MainThread;

@SuppressWarnings("unused")
public interface QEDPageStreamReceiver<T> extends QEDPageReceiver<T> {
    @MainThread
    default void onProgressUpdate(T obj, long done, long total) {}
}