package eu.jonahbauer.qed.network.parser.database;

import android.util.Log;
import androidx.annotation.NonNull;
import eu.jonahbauer.qed.model.Event;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.List;

public final class EventListParser extends DatabaseParser<List<Event>> {
    private static final String LOG_TAG = EventListParser.class.getName();
    public static final EventListParser INSTANCE = new EventListParser();

    private EventListParser() {}

    @NonNull
    @Override
    protected List<Event> parse(@NonNull List<Event> list, @NonNull Document document) {
        list.clear();

        for (var tr : document.select("#events_table tbody tr")) {
            try {
                Elements columns = tr.select("td");

                long id = parseIdFromHref(columns.get(1).selectFirst("a"), Event.NO_ID);
                Event event = new Event(id);

                parseString(columns.get(1), event::setTitle);
                parseLocalDate(columns.get(2), event::setStart);
                parseLocalDate(columns.get(3), event::setEnd);
                parseString(columns.get(4), event::setHotel);
                parseLocalDate(columns.get(5), event::setDeadline);
                parseCurrency(columns.get(6), event::setCost);
                parseInteger(columns.get(7), event::setMaxParticipants);

                list.add(event);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error parsing event list.", e);
            }
        }

        return list;
    }
}
