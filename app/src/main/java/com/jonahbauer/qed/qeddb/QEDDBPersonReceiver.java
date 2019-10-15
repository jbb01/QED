package com.jonahbauer.qed.qeddb;

import com.jonahbauer.qed.qeddb.person.Person;

public interface QEDDBPersonReceiver {
    void onPersonReceived(Person person);
}
