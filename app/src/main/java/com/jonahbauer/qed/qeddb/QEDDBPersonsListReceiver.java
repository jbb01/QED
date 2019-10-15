package com.jonahbauer.qed.qeddb;

import com.jonahbauer.qed.qeddb.person.Person;

import java.util.List;

public interface QEDDBPersonsListReceiver {
    void onPersonsListReceived(List<Person> persons);
}
