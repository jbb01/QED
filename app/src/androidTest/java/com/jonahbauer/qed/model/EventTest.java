package com.jonahbauer.qed.model;

import android.os.Parcel;
import com.jonahbauer.qed.model.util.ParsedLocalDate;
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
        event.setStart(new ParsedLocalDate("01.01.2000", LocalDate.of(2000, 1, 1)));
        event.setEnd(new ParsedLocalDate("31.12.2000", LocalDate.of(2000, 12, 31)));
        event.setDeadline(new ParsedLocalDate("01.01.1999", LocalDate.of(1999, 1, 1)));
        event.setHotel("JH Musterstadt");
        event.setHotelAddress("Musterstraße 10\n12345 Musterstadt");
        event.setEmailOrga("musterstadt-orga@example.com");
        event.setEmailAll("musterstadt-teilnehmer@example.com");
        event.setLoaded(Instant.now());

        var registration = new Registration(1);
        registration.setStatus(Registration.Status.CONFIRMED);
        registration.setOrganizer(true);
        registration.setPersonName("Max Mustermann");
        event.getParticipants().add(registration);

        return event;
    }
}
