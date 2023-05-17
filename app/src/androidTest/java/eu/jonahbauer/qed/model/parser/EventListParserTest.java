package eu.jonahbauer.qed.model.parser;

import eu.jonahbauer.qed.model.Event;
import eu.jonahbauer.qed.networking.NetworkUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Objects;

import static org.junit.Assert.*;

public class EventListParserTest {

    @Test
    public void parse() throws IOException {
        String html;
        try (var in = getClass().getResourceAsStream("/event_list.html")) {
            html = new String(NetworkUtils.readAllBytes(Objects.requireNonNull(in)), StandardCharsets.UTF_8);
        }

        var list = new ArrayList<Event>();

        var parser = EventListParser.INSTANCE;
        parser.apply(list, html);

        assertEquals(4, list.size());

        assertEquals(5, list.get(0).getId());
        assertEquals("Erlangen 2011", list.get(0).getTitle());
        assertEquals((Double) 55.0, list.get(0).getCost());
        assertEquals((Integer) 40, list.get(0).getMaxParticipants());
        assertNotNull(list.get(0).getStart());
        assertNotNull(list.get(0).getEnd());
        assertNotNull(list.get(0).getDeadline());
        assertEquals(LocalDate.of(2011, 7, 7), list.get(0).getStart().getLocalDate());
        assertEquals(LocalDate.of(2011, 7, 10), list.get(0).getEnd().getLocalDate());
        assertEquals(LocalDate.of(2011, 5, 10), list.get(0).getDeadline().getLocalDate());

        assertEquals(3, list.get(1).getId());
        assertEquals("Passau 2011", list.get(1).getTitle());
        assertEquals((Double) 110.0, list.get(1).getCost());
        assertEquals((Integer) 30, list.get(1).getMaxParticipants());
        assertNotNull(list.get(1).getStart());
        assertNotNull(list.get(1).getEnd());
        assertNotNull(list.get(1).getDeadline());
        assertEquals(LocalDate.of(2011, 4, 18), list.get(1).getStart().getLocalDate());
        assertEquals(LocalDate.of(2011, 4, 21), list.get(1).getEnd().getLocalDate());
        assertEquals(LocalDate.of(2011, 4, 20), list.get(1).getDeadline().getLocalDate());

        assertEquals(1, list.get(2).getId());
        assertEquals("Ulm 2010", list.get(2).getTitle());
        assertEquals((Double) 0.0, list.get(2).getCost());
        assertEquals((Integer) 2, list.get(2).getMaxParticipants());
        assertNotNull(list.get(2).getStart());
        assertNotNull(list.get(2).getEnd());
        assertNull(list.get(2).getDeadline());
        assertEquals(LocalDate.of(2010, 9, 17), list.get(2).getStart().getLocalDate());
        assertEquals(LocalDate.of(2010, 9, 20), list.get(2).getEnd().getLocalDate());

        assertEquals(2, list.get(3).getId());
        assertEquals("Tastseminar", list.get(3).getTitle());
        assertEquals((Double) 50.0, list.get(3).getCost());
        assertEquals((Integer) 80, list.get(3).getMaxParticipants());
        assertNotNull(list.get(3).getStart());
        assertNotNull(list.get(3).getEnd());
        assertNotNull(list.get(3).getDeadline());
        assertEquals(LocalDate.of(2000, 9, 3), list.get(3).getStart().getLocalDate());
        assertEquals(LocalDate.of(2000, 9, 10), list.get(3).getEnd().getLocalDate());
        assertEquals(LocalDate.of(2100, 10, 1), list.get(3).getDeadline().getLocalDate());
    }
}
