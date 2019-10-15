package com.jonahbauer.qed.logs;

import androidx.annotation.Nullable;

import com.jonahbauer.qed.chat.Message;

import java.util.List;

@Deprecated
public interface LogReceiver {
    void onReceiveLogs(@Nullable List<Message> message);
    void onLogError();

    void onOutOfMemory();
}
