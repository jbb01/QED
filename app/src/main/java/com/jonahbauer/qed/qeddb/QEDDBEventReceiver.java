package com.jonahbauer.qed.qeddb;

import com.jonahbauer.qed.qeddb.event.Event;

public interface QEDDBEventReceiver {
    void onEventReceived(Event event);
}
