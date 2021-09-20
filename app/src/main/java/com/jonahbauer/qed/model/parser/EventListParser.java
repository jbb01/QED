package com.jonahbauer.qed.model.parser;

import android.util.Log;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.model.Event;
import com.jonahbauer.qed.networking.parser.HtmlParser;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.Date;
import java.util.List;
import java.util.Objects;

public final class EventListParser extends HtmlParser<List<Event>> {
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
