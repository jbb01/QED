package com.jonahbauer.qed.util;

import androidx.core.util.Pair;

import com.jonahbauer.qed.model.Album;
import com.jonahbauer.qed.model.Event;
import com.jonahbauer.qed.model.Image;
import com.jonahbauer.qed.model.Person;
import com.jonahbauer.qed.model.Registration;

import java.util.Calendar;
import java.util.GregorianCalendar;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Debug {
    public static Person dummyPerson() {
        Person person = new Person(-1);
        person.setFirstName("Max");
        person.setLastName("Mustermann");
        person.setEmail("max.mustermann@example.org");
        person.setBirthday(new GregorianCalendar(2000, Calendar.JANUARY, 1).getTime());
        person.setBirthdayString("01.01.2000");
        person.setHomeStation("Musterstadt");
        person.setRailcard("Bahncard 42");
        person.setFood("Nur leckeres Essen");
        person.setNotes("Raum für Notizen");
        person.setMember(true);
        person.setActive(true);
        person.setDateOfJoining(new GregorianCalendar(2020, Calendar.MONTH, 14).getTime());
        person.setDateOfJoiningString("03/14/2020");
        person.setLeavingDate(new GregorianCalendar(2050, Calendar.DECEMBER, 31).getTime());
        person.setLeavingDateString("31-12-2050");
        person.getContacts().add(Pair.create("mobil", "0123456789"));
        person.getContacts().add(Pair.create("daheim", "987654321"));
        person.getContacts().add(Pair.create("skype", "max.mustermann"));
        person.getAddresses().add("Musterstraße 10\n12345 Musterstadt");
        person.getAddresses().add("Mustergasse 5\n54321 Musterdorf");

        Registration registration;

        registration = new Registration(-1);
        registration.setStatus(Registration.Status.OPEN);
        person.getEvents().put("Akademie 2021", registration);

        registration = new Registration(-1);
        registration.setStatus(Registration.Status.CONFIRMED);
        person.getEvents().put("Musterstadt 2020", registration);

        registration = new Registration(-1);
        registration.setStatus(Registration.Status.CANCELLED);
        person.getEvents().put("Musterstadt 2019", registration);

        registration = new Registration(-1);
        registration.setStatus(Registration.Status.CONFIRMED);
        registration.setOrganizer(true);
        person.getEvents().put("Testseminar", registration);

        person.setLoaded(true);
        return person;
    }

    public static Event dummyEvent() {
        Event event = new Event(-1);
        event.setTitle("Musterveranstaltung");
        event.setCost(200);
        event.setNotes("Raum für Notizen");
        event.setMaxParticipants(50);

        event.setStart(new GregorianCalendar(2021, Calendar.AUGUST, 1).getTime());
        event.setStartString("01.08.2021");

        event.setEnd(new GregorianCalendar(2021, Calendar.AUGUST, 10).getTime());
        event.setEndString("10.08.2021");

        event.setDeadline(new GregorianCalendar(2021, Calendar.JULY, 16).getTime());
        event.setDeadlineString("16.07.2021");

        event.setHotel("Musterunterkunft");
        event.setHotelAddress("Musterstraße 10\n12345 Musterstadt");

        event.setEmailAll("musterveranstaltung-teilnehmer@example.org");
        event.setEmailOrga("musterveranstaltung@example.org");

        Registration registration;
        registration = new Registration(-1);
        registration.setStatus(Registration.Status.CONFIRMED);
        registration.setOrganizer(true);
        event.getParticipants().put("Max Mustermann", registration);

        registration = new Registration(-1);
        registration.setStatus(Registration.Status.OPEN);
        registration.setOrganizer(true);
        event.getParticipants().put("Erika Musterfrau", registration);

        for (int i = 0; i < 10; i++) {
            registration = new Registration(-1);
            registration.setStatus(
                    Math.random() > 0.5 ? Registration.Status.OPEN
                            : Math.random() > 0 ? Registration.Status.CONFIRMED
                            : Registration.Status.CANCELLED
            );
            event.getParticipants().put("Testteilnehmer " + i, registration);
        }

        event.setLoaded(true);
        return event;
    }

    public static Album dummyAlbum() {
        Album album = new Album(-1);
        album.setName("Album");
        album.setOwner("Max Mustermann");
        album.setCreationDate("01.01.2000");
        album.setPrivate_(true);
        album.setImageListDownloaded(true);
        album.setLoaded(true);
        album.getCategories().add("Kategorie 1");
        album.getCategories().add("Kategorie 2");
        album.getCategories().add("Kategorie 3");
        album.getCategories().add("Sonstige");
        album.getPersons().add(dummyPerson());
        for (int i = 1; i < 6; i++) {
            album.getDates().add(new GregorianCalendar(2000, Calendar.JANUARY, i).getTime());
        }
        for (int i = 0; i < 10; i++) {
            album.getImages().add(new Image(-1));
        }
        return album;
    }
}
