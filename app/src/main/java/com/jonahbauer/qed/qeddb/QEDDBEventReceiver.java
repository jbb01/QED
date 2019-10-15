package com.jonahbauer.qed.qeddb;

import com.jonahbauer.qed.qeddb.event.Event;


/**
 * @deprecated
 * use {@link com.jonahbauer.qed.networking.QEDPageReceiver} instead
 */
@Deprecated
public interface QEDDBEventReceiver {
    void onEventReceived(Event event);
}
