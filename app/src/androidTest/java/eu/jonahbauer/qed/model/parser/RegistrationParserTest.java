package eu.jonahbauer.qed.model.parser;

import eu.jonahbauer.qed.model.Person;
import eu.jonahbauer.qed.model.Registration;
import eu.jonahbauer.qed.networking.NetworkUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

import static org.junit.Assert.*;

public class RegistrationParserTest {

    @Test
    public void parseOther() throws IOException {
        String html;
        try (var in = getClass().getResourceAsStream("/registration.html")) {
            html = new String(NetworkUtils.readAllBytes(Objects.requireNonNull(in)), StandardCharsets.UTF_8);
        }

        var registration = new Registration(117);

        var parser = RegistrationParser.INSTANCE;
        parser.apply(registration, html);

        basicChecks(registration);
    }

    @Test
    public void parseSelf() throws IOException {
        String html;
        try (var in = getClass().getResourceAsStream("/registration_self.html")) {
            html = new String(NetworkUtils.readAllBytes(Objects.requireNonNull(in)), StandardCharsets.UTF_8);
        }

        var registration = new Registration(117);

        var parser = RegistrationParser.INSTANCE;
        parser.apply(registration, html);

        basicChecks(registration);
        selfChecks(registration);
    }

    private void basicChecks(Registration registration) {
        assertEquals(Registration.Status.CONFIRMED, registration.getStatus());
        assertEquals(false, registration.getOrganizer());

        assertEquals(2, registration.getEventId());
        assertEquals("Tastseminar", registration.getEventTitle());

        assertEquals(349, registration.getPersonId());
        assertEquals("Test User", registration.getPersonName());
        assertNotNull(registration.getPersonBirthday());
        assertEquals(LocalDate.of(2001, 2, 3), registration.getPersonBirthday().getLocalDate());
        assertEquals(Person.Gender.MALE, registration.getPersonGender());
        assertEquals("test.user@example.org", registration.getPersonMail());
        assertEquals("Musterstraße 12\n98765 Musterstadt", registration.getPersonAddress());
        assertEquals("0123/456789", registration.getPersonPhone());

        assertNotNull(registration.getTimeOfArrival());
        assertEquals(OffsetDateTime.of(2021, 12, 3, 0, 0, 0, 0, ZoneOffset.ofHours(1)).toInstant(), registration.getTimeOfArrival().getInstant());
        assertNotNull(registration.getTimeOfDeparture());
        assertEquals(OffsetDateTime.of(2021, 12, 6, 0, 0, 0, 0, ZoneOffset.ofHours(1)).toInstant(), registration.getTimeOfDeparture().getInstant());
        assertEquals("Musterstadt", registration.getSourceStation());
        assertEquals("Musterkaff", registration.getTargetStation());
        assertEquals("100", registration.getRailcard());
        assertEquals(3, (Object) registration.getOvernightStays());

        assertEquals("vegetarier", registration.getFood());
        assertNull(registration.getTalks());
        assertNull(registration.getNotes());
    }

    private void selfChecks(Registration registration) {
        assertEquals(100d, (Object) registration.getPaymentAmount());
        assertEquals(true, registration.getPaymentDone());
        assertNotNull(registration.getPaymentTime());
        assertEquals(LocalDate.of(2021, 12, 6), registration.getPaymentTime().getLocalDate());
        assertEquals(true, registration.getMemberAbatement());
        assertNull(registration.getOtherAbatement());
    }
}
