package eu.jonahbauer.qed.model.parser;

import androidx.core.util.Pair;
import eu.jonahbauer.qed.model.Person;
import eu.jonahbauer.qed.network.util.NetworkUtil;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

import static org.junit.Assert.*;

public class PersonParserTest {

    @Test
    public void parseOther() throws IOException {
        String html;
        try (var in = getClass().getResourceAsStream("/person.html")) {
            html = new String(NetworkUtil.readAllBytes(Objects.requireNonNull(in)), StandardCharsets.UTF_8);
        }

        var person = new Person(349);

        var parser = PersonParser.INSTANCE;
        parser.apply(person, html);

        basicChecks(person);
    }

    @Test
    public void parseSelf() throws IOException {
        String html;
        try (var in = getClass().getResourceAsStream("/person_self.html")) {
            html = new String(NetworkUtil.readAllBytes(Objects.requireNonNull(in)), StandardCharsets.UTF_8);
        }

        var person = new Person(349);

        var parser = PersonParser.INSTANCE;
        parser.apply(person, html);

        basicChecks(person);
        selfChecks(person);
    }

    private void basicChecks(Person person) {
        assertEquals("Test", person.getFirstName());
        assertEquals("User", person.getLastName());
        assertEquals("test.user@example.org", person.getEmail());

        assertNotNull(person.getBirthday());
        assertEquals(LocalDate.of(2001, 2, 3), person.getBirthday().getLocalDate());

        assertEquals(Person.Gender.MALE, person.getGender());

        assertNotNull(person.getDateOfJoining());
        assertEquals(LocalDate.of(2020, 4, 5), person.getDateOfJoining().getLocalDate());

        assertNull(person.getDateOfQuitting());
        assertEquals(true, person.getMember());
        assertEquals(true, person.getActive());
        assertEquals("Musterstadt", person.getHomeStation());
        assertEquals("Bahncard 100", person.getRailcard());
        assertNull(person.getFood());
        assertNull(person.getNotes());

        assertEquals(2, person.getAddresses().size());
        assertEquals(Set.of(Pair.create("daheim", "09876 54321"), Pair.create("mobil", "0123/456789")), person.getContacts());
        assertEquals(1, person.getEvents().size());
    }

    private void selfChecks(Person person) {
        assertNotNull(person.getMemberUntil());
        assertEquals(LocalDate.of(2024, 12, 31), person.getMemberUntil().getLocalDate());
        assertNotNull(person.getPaidUntil());
        assertEquals(LocalDate.of(2022, 12, 31), person.getPaidUntil().getLocalDate());

        assertEquals(3, person.getPayments().size());
        assertTrue(person.getGroups().contains("Mitglieder"));
        assertTrue(person.getGroups().contains("Newsletter an"));
        assertTrue(person.getGroups().contains("QED-Chat-Users"));
        assertTrue(person.getGroups().contains("QED-Gallery-Editors"));
        assertTrue(person.getGroups().contains("QED-Gallery-Viewers"));
        assertTrue(person.getGroups().contains("QED-Kalender"));

        assertNotNull(person.getPrivacy());
        assertTrue(person.getPrivacy().contains(Person.Privacy.PHOTOS));
        assertTrue(person.getPrivacy().contains(Person.Privacy.PUBLIC_BIRTHDAY));
        assertTrue(person.getPrivacy().contains(Person.Privacy.PUBLIC_EMAIL));
        assertTrue(person.getPrivacy().contains(Person.Privacy.PUBLIC_ADDRESS));
        assertTrue(person.getPrivacy().contains(Person.Privacy.NEWSLETTER));
        assertTrue(person.getPrivacy().contains(Person.Privacy.PUBLIC_PROFILE));
    }
}
