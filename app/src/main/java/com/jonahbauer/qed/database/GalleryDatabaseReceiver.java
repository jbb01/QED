package com.jonahbauer.qed.database;

import java.util.List;

public interface GalleryDatabaseReceiver {
    void onReceiveResult(List items);
    void onDatabaseError();

    void onInsertAllUpdate(int done, int total);
}
