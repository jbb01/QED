package com.jonahbauer.qed.model;

import android.os.Parcel;
import com.jonahbauer.qed.model.util.ParsedLocalDate;
import org.junit.Test;

import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.Assert.assertEquals;

public class EventTest {

    @Test
    public void testParcelableConsistentWithEquals() {
        var event = dummyEvent();
        var copy = cloneWithParcelable(event);
        assertEquals(event, copy);
    }

    @Test
    public void testParcelableContainsAllNonTransientFields() throws IllegalAccessException {
        var event = dummyEvent();
        var copy = cloneWithParcelable(event);

        var fields = Event.class.getDeclaredFields();
        for (var field : fields) {
            field.setAccessible(true);
            if ((field.getModifiers() & Modifier.STATIC) != 0) continue;
            if ((field.getModifiers() & Modifier.TRANSIENT) != 0) continue;
            assertEquals(field.getName(), field.get(event), field.get(copy));
        }
    }

    private Event cloneWithParcelable(Event event) {
        var parcel = Parcel.obtain();
        try {
            event.writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            return Event.CREATOR.createFromParcel(parcel);
        } finally {
            parcel.recycle();
        }
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
