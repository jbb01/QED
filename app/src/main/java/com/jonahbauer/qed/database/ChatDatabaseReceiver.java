package com.jonahbauer.qed.database;

import com.jonahbauer.qed.chat.Message;

import java.util.List;

public interface ChatDatabaseReceiver {
    void onReceiveResult(List<Message> messages);
    void onDatabaseError();

    void onInsertAllUpdate(int done, int total);
}
