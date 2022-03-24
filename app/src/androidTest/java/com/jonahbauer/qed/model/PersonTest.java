package com.jonahbauer.qed.model;

import android.os.Parcel;
import androidx.core.util.Pair;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.Assert.assertEquals;

public class PersonTest {

    @Test
    public void testParcelableConsistentWithEquals() {
        var person = dummyPerson();

        var parcel = Parcel.obtain();
        person.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);

        var out = Person.CREATOR.createFromParcel(parcel);
        assertEquals(person, out);
    }

    private static Person dummyPerson() {
        var person = new Person(1);
        person.setUsername("MaxMustermann");
        person.setFirstName("Max");
        person.setLastName("Mustermann");
        person.setFullName("Max Mustermann");
        person.setEmail("max.mustermann@example.org");
        person.setGender("männlich");
        person.setBirthdayString("01.01.2000");
        person.setBirthday(LocalDate.of(2000, 1, 1));
        person.setHomeStation("Musterstadt");
        person.setRailcard("50");
        person.setFood("Steine");
        person.setNotes("Raum für Notizen");
        person.setMember(true);
        person.setActive(true);
        person.setDateOfJoiningString("01.01.2018");
        person.setDateOfJoining(LocalDate.of(2018, 1, 1));
        person.setLeavingDateString("01.01.2019");
        person.setLeavingDate(LocalDate.of(2019, 1, 1));
        person.setLoaded(Instant.now());
        person.getContacts().add(Pair.create("mobil", "0123456789"));
        person.getContacts().add(Pair.create("daheim", "987654321"));
        person.getAddresses().add("Musterstraße 1\n12345 Musterstadt");

        var registration = new Registration(1);
        registration.setStatus(Registration.Status.CONFIRMED);
        registration.setOrganizer(true);
        registration.setEventTitle("Musterveranstaltung");
        person.getEvents().add(registration);

        return person;
    }
}
