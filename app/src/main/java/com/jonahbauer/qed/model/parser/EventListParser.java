package com.jonahbauer.qed.model.parser;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.model.Event;
import com.jonahbauer.qed.networking.parser.HtmlParser;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class EventListParser extends HtmlParser<List<Event>> {
    public static final EventListParser INSTANCE = new EventListParser();

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY);

    private EventListParser() {}

    @NonNull
    @Override
    protected List<Event> parse(@NonNull List<Event> list, Document document) {
        list.clear();

        document.select("#events_table tbody tr")
                .stream()
                .map(tr -> {
                    Elements columns = tr.select("td");

                    String title = columns.get(1).select("a").text();
                    String startString = columns.get(2).select("time").text();
                    String endString = columns.get(3).select("time").text();

                    Date start = parseDate(columns.get(2).select("time").attr("datetime"));
                    Date end = parseDate(columns.get(3).select("time").attr("datetime"));

                    long id = Long.parseLong(columns.get(1).select("a").attr("href").substring("/events/".length()));

                    Event event = new Event(id);
                    event.setTitle(title);
                    event.setStartString(startString);
                    event.setEndString(endString);
                    event.setStart(start);
                    event.setEnd(end);

                    return event;
                }).forEach(list::add);

        return list;
    }
}
