package com.jonahbauer.qed.model;

import android.os.Parcel;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.Assert.assertEquals;

public class EventTest {

    @Test
    public void testParcelableConsistentWithEquals() {
        var event = dummyEvent();

        var parcel = Parcel.obtain();
        event.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);

        var out = Event.CREATOR.createFromParcel(parcel);
        assertEquals(event, out);
    }

    private static Event dummyEvent() {
        var event = new Event(1);
        event.setTitle("Musterveranstaltung");
        event.setCost(100.5d);
        event.setNotes("Raum für Notizen");
        event.setMaxParticipants(10);
        event.setStartString("01.01.2000");
        event.setStart(LocalDate.of(2000, 1, 1));
        event.setEndString("31.12.2000");
        event.setEnd(LocalDate.of(2000, 12, 31));
        event.setDeadlineString("01.01.1999");
        event.setDeadline(LocalDate.of(1999, 1, 1));
        event.setHotel("JH Musterstadt");
        event.setHotelAddress("Musterstraße 10\n12345 Musterstadt");
        event.setEmailOrga("musterstadt-orga@example.com");
        event.setEmailAll("musterstadt-teilnehmer@example.com");
        event.setLoaded(Instant.now());

        var registration = new Registration(1);
        registration.setStatus(Registration.Status.CONFIRMED);
        registration.setOrganizer(true);
        event.getParticipants().put("Max Mustermann", registration);

        return event;
    }
}
