package com.jonahbauer.qed.qeddb;

import com.jonahbauer.qed.qeddb.event.Event;

import java.util.List;

public interface QEDDBEventsListReceiver {
    void onEventsListReceived(List<Event> event);
}
