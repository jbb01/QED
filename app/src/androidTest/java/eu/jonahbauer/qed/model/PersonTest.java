package eu.jonahbauer.qed.model;

import android.os.Parcel;
import androidx.core.util.Pair;

import eu.jonahbauer.qed.model.contact.ContactDetail;
import eu.jonahbauer.qed.model.util.ParsedLocalDate;
import org.junit.Test;

import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;

import static org.junit.Assert.assertEquals;

public class PersonTest {

    @Test
    public void testParcelableConsistentWithEquals() {
        var person = dummyPerson();
        var copy = cloneWithParcelable(person);
        assertEquals(person, copy);
    }

    @Test
    public void testParcelableContainsAllNonTransientFields() throws IllegalAccessException {
        var person = dummyPerson();
        var copy = cloneWithParcelable(person);

        var fields = Person.class.getDeclaredFields();
        for (var field : fields) {
            field.setAccessible(true);
            if ((field.getModifiers() & Modifier.STATIC) != 0) continue;
            if ((field.getModifiers() & Modifier.TRANSIENT) != 0) continue;
            assertEquals(field.getName(), field.get(person), field.get(copy));
        }
    }

    private Person cloneWithParcelable(Person person) {
        var parcel = Parcel.obtain();
        try {
            person.writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            return Person.CREATOR.createFromParcel(parcel);
        } finally {
            parcel.recycle();
        }
    }

    private static Person dummyPerson() {
        var person = new Person(1);
        person.setUsername("MaxMustermann");
        person.setFirstName("Max");
        person.setLastName("Mustermann");
        person.setFullName("Max Mustermann");
        person.setEmail("max.mustermann@example.org");
        person.setGender(Person.Gender.MALE);
        person.setBirthday(new ParsedLocalDate("01.01.2000", LocalDate.of(2000, 1, 1)));
        person.setHomeStation("Musterstadt");
        person.setRailcard("50");
        person.setFood("Steine");
        person.setNotes("Raum für Notizen");
        person.setMember(true);
        person.setActive(true);
        person.setDateOfJoining(new ParsedLocalDate("01.01.2018", LocalDate.of(2018, 1, 1)));
        person.setDateOfQuitting(new ParsedLocalDate("01.01.2019", LocalDate.of(2019, 1, 1)));
        person.setPaidUntil(new ParsedLocalDate("31.12.2022", LocalDate.of(2022, 12, 31)));
        person.setMemberUntil(new ParsedLocalDate("31.12.2024", LocalDate.of(2024, 12, 31)));
        person.setLoaded(Instant.now());
        person.getContacts().add(new ContactDetail("mobil", "0123456789"));
        person.getContacts().add(new ContactDetail("daheim", "987654321"));
        person.getAddresses().add("Musterstraße 1\n12345 Musterstadt");
        person.getGroups().add("QED-Gallery-Viewers");
        person.getGroups().add("QED-Gallery-Editors");
        person.setPrivacy(EnumSet.of(Person.Privacy.NEWSLETTER, Person.Privacy.PUBLIC_PROFILE));

        var registration = new Registration(1);
        registration.setStatus(Registration.Status.CONFIRMED);
        registration.setOrganizer(true);
        registration.setEventTitle("Musterveranstaltung");
        person.getEvents().add(registration);

        return person;
    }
}
