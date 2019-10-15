package com.jonahbauer.qed.logs;

import com.jonahbauer.qed.chat.Message;

import java.util.List;

public interface LogReceiver {
    void onReceiveLogs(List<Message> messages);
    void onLogError();
}
