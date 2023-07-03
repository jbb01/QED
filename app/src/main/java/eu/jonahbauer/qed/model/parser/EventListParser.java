package eu.jonahbauer.qed.model.parser;

import android.util.Log;

import androidx.annotation.NonNull;

import eu.jonahbauer.qed.model.Event;

import eu.jonahbauer.qed.networking.parser.HtmlParser;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.List;
import java.util.Objects;

import static eu.jonahbauer.qed.model.parser.EventParser.parseCost;

public final class EventListParser extends DatabaseParser<List<Event>> {
    private static final String LOG_TAG = EventListParser.class.getName();
    public static final EventListParser INSTANCE = new EventListParser();

    private EventListParser() {}

    @NonNull
    @Override
    protected List<Event> parse(@NonNull List<Event> list, Document document) {
        list.clear();

        document.select("#events_table tbody tr")
                .stream()
                .map(tr -> {
                    try {
                        Elements columns = tr.select("td");

                        long id = parseIdFromHref(columns.get(1).selectFirst("a"), Event.NO_ID);
                        Event event = new Event(id);

                        title: try {
                            var element = columns.get(1).selectFirst("a");
                            if (element == null) break title;
                            event.setTitle(element.text());
                        } catch (Exception ignored) {}

                        start: try {
                            var element = columns.get(2).selectFirst("time");
                            if (element == null) break start;
                            event.setStart(HtmlParser.parseLocalDate(element));
                        } catch (Exception ignored) {}

                        end: try {
                            var element = columns.get(3).selectFirst("time");
                            if (element == null) break end;
                            event.setEnd(HtmlParser.parseLocalDate(element));
                        } catch (Exception ignored) {}

                        hotel: try {
                            var element = columns.get(4).selectFirst("a");
                            if (element == null) break hotel;
                            event.setHotel(element.text());
                        } catch (Exception ignored) {}

                        deadline: try {
                            var element = columns.get(5).selectFirst("time");
                            if (element == null) break deadline;
                            event.setDeadline(HtmlParser.parseLocalDate(element));
                        } catch (Exception ignored) {}

                        cost: try {
                            var element = columns.get(6);
                            if (element == null) break cost;
                            event.setCost(parseCost(element));
                        } catch (Exception ignored) {}

                        maxParticipants: try {
                            var element = columns.get(7);
                            if (element == null) break maxParticipants;
                            event.setMaxParticipants(HtmlParser.parseInteger(element));
                        } catch (NumberFormatException ignored) {}

                        return event;
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Error parsing event list.", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .forEach(list::add);

        return list;
    }
}
