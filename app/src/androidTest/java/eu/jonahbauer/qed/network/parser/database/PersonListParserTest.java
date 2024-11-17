package eu.jonahbauer.qed.network.parser.database;

import eu.jonahbauer.qed.model.Person;
import eu.jonahbauer.qed.network.util.NetworkUtil;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Objects;

import static org.junit.Assert.*;

public class PersonListParserTest {

    @Test
    public void parse() throws IOException {
        String html;
        try (var in = getClass().getResourceAsStream("/person_list.html")) {
            html = new String(NetworkUtil.readAllBytes(Objects.requireNonNull(in)), StandardCharsets.UTF_8);
        }

        var list = new ArrayList<Person>();

        var parser = PersonListParser.INSTANCE;
        parser.apply(list, html);

        assertEquals(4, list.size());

        assertEquals(0, list.get(0).getId());
        assertEquals("Stefani", list.get(0).getFirstName());
        assertEquals("Rivas", list.get(0).getLastName());
        assertEquals(Person.Gender.MALE, list.get(0).getGender());
        assertNotNull(list.get(0).getBirthday());
        assertEquals(LocalDate.of(1983, 6, 29), list.get(0).getBirthday().getLocalDate());
        assertEquals("stefani.rivas@example.org", list.get(0).getEmail());
        assertEquals("StefaniRivas", list.get(0).getUsername());
        assertEquals(true, list.get(0).getActive());
        assertEquals(false, list.get(0).getMember());
        assertNotNull(list.get(0).getDateOfJoining());
        assertEquals(LocalDate.of(2000, 1, 1), list.get(0).getDateOfJoining().getLocalDate());
        assertNull(list.get(0).getDateOfQuitting());

        assertEquals(1, list.get(1).getId());
        assertEquals("Assol", list.get(1).getFirstName());
        assertEquals("Vespa", list.get(1).getLastName());
        assertEquals(Person.Gender.MALE, list.get(1).getGender());
        assertNotNull(list.get(1).getBirthday());
        assertEquals(LocalDate.of(2002, 10, 7), list.get(1).getBirthday().getLocalDate());
        assertEquals("assol.vespa@example.org", list.get(1).getEmail());
        assertEquals("AssolVespa", list.get(1).getUsername());
        assertEquals(true, list.get(1).getActive());
        assertEquals(false, list.get(1).getMember());
        assertNotNull(list.get(1).getDateOfJoining());
        assertEquals(LocalDate.of(2017, 6, 14), list.get(1).getDateOfJoining().getLocalDate());
        assertNull(list.get(1).getDateOfQuitting());

        assertEquals(2, list.get(2).getId());
        assertEquals("Natasha", list.get(2).getFirstName());
        assertEquals("Susskind", list.get(2).getLastName());
        assertEquals(Person.Gender.MALE, list.get(2).getGender());
        assertNotNull(list.get(2).getBirthday());
        assertEquals(LocalDate.of(2001, 12, 2), list.get(2).getBirthday().getLocalDate());
        assertEquals("natasha.susskind@example.org", list.get(2).getEmail());
        assertEquals("NatashaSusskind", list.get(2).getUsername());
        assertEquals(true, list.get(2).getActive());
        assertEquals(true, list.get(2).getMember());
        assertNotNull(list.get(2).getDateOfJoining());
        assertEquals(LocalDate.of(2017, 6, 24), list.get(2).getDateOfJoining().getLocalDate());
        assertNull(list.get(2).getDateOfQuitting());

        assertEquals(3, list.get(3).getId());
        assertEquals("Rayna", list.get(3).getFirstName());
        assertEquals("Oláh", list.get(3).getLastName());
        assertEquals(Person.Gender.FEMALE, list.get(3).getGender());
        assertNull(list.get(3).getBirthday());
        assertNull(list.get(3).getEmail());
        assertEquals("RaynaOlah", list.get(3).getUsername());
        assertEquals(true, list.get(3).getActive());
        assertEquals(true, list.get(3).getMember());
        assertNotNull(list.get(3).getDateOfJoining());
        assertEquals(LocalDate.of(2015, 2, 28), list.get(3).getDateOfJoining().getLocalDate());
        assertNull(list.get(3).getDateOfQuitting());
    }
}
