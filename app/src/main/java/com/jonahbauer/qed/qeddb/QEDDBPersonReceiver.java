package com.jonahbauer.qed.qeddb;

import com.jonahbauer.qed.qeddb.person.Person;

/**
 * @deprecated
 * use {@link com.jonahbauer.qed.networking.QEDPageReceiver} instead
 */
@Deprecated
public interface QEDDBPersonReceiver {
    void onPersonReceived(Person person);
}
