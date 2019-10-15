package com.jonahbauer.qed.qeddb;

import com.jonahbauer.qed.qeddb.person.Person;

import java.util.List;

/**
 * @deprecated
 * use {@link com.jonahbauer.qed.networking.QEDPageReceiver} instead
 */
@Deprecated
public interface QEDDBPersonsListReceiver {
    void onPersonsListReceived(List<Person> persons);
    void onPersonsListError();
}
