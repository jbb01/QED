package com.jonahbauer.qed.qeddb;

import com.jonahbauer.qed.qeddb.event.Event;

import java.util.List;


/**
 * @deprecated
 * use {@link com.jonahbauer.qed.networking.QEDPageReceiver} instead
 */
@Deprecated
public interface QEDDBEventsListReceiver {
    void onEventsListReceived(List<Event> event);
}
