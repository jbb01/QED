package eu.jonahbauer.qed.model.parser;

import eu.jonahbauer.qed.model.Event;
import eu.jonahbauer.qed.network.util.NetworkUtil;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Objects;

import static org.junit.Assert.*;

public class EventParserTest {

    @Test
    public void parse() throws IOException {
        String html;
        try (var in = getClass().getResourceAsStream("/event.html")) {
            html = new String(NetworkUtil.readAllBytes(Objects.requireNonNull(in)), StandardCharsets.UTF_8);
        }

        var event = new Event(2);

        var parser = EventParser.INSTANCE;
        parser.apply(event, html);

        basicChecks(event);
    }

    private void basicChecks(Event event) {
        assertEquals("Tastseminar", event.getTitle());
        assertEquals((Double) 50.00, event.getCost());
        assertEquals("Test", event.getNotes());
        assertEquals((Integer) 80, event.getMaxParticipants());

        assertNotNull(event.getStart());
        assertEquals(LocalDate.of(2000, 9, 3), event.getStart().getLocalDate());
        assertNotNull(event.getEnd());
        assertEquals(LocalDate.of(2000, 9, 10), event.getEnd().getLocalDate());
        assertNotNull(event.getDeadline());
        assertEquals(LocalDate.of(2100, 10, 1), event.getDeadline().getLocalDate());

        assertNull(event.getHotel());
        assertNull(event.getHotelAddress());

        assertEquals("tast-orgas@example.org", event.getEmailOrga());
        assertEquals("tast-teilnehmer@example.org", event.getEmailAll());

        assertEquals(0, event.getOrganizers().size());
        assertEquals(31, event.getParticipants().size());
    }
}
