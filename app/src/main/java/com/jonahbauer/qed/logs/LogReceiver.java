package com.jonahbauer.qed.logs;

import android.support.annotation.Nullable;

import com.jonahbauer.qed.chat.Message;

import java.util.List;

public interface LogReceiver {
    void onReceiveLogs(@Nullable List<Message> message);
    void onLogError();

    void onOutOfMemory();
}
