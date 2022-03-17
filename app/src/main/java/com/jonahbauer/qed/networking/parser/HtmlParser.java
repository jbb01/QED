package com.jonahbauer.qed.networking.parser;

import android.util.Log;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Contract;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

public abstract class HtmlParser<T> implements Parser<T> {
    private static final String LOG_TAG = HtmlParser.class.getName();

    @Override
    public final T apply(T obj, String s) {
        Document document = Jsoup.parse(s);
        return parse(obj, document);
    }

    @NonNull
    @Contract("_, _ -> param1")
    protected abstract T parse(@NonNull T obj, Document document) throws HtmlParseException;

    protected LocalDate parseLocalDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            Log.w(LOG_TAG, "Could not parse local date \"" + date + "\".", e);
            return null;
        }
    }

    protected LocalDate parseLocalDate(Element time) {
        return parseLocalDate(time.attr("datetime"));
    }

    protected Instant parseInstant(String instant) {
        try {
            return OffsetDateTime.parse(instant).toInstant();
        } catch (DateTimeParseException e) {
            Log.w(LOG_TAG, "Could not parse instant \"" + instant + "\".", e);
            return null;
        }
    }

    protected Instant parseInstant(Element time) {
        return parseInstant(time.attr("datetime"));
    }

    protected boolean parseBoolean(String bool) {
        return "Ja".equals(bool);
    }

    protected boolean parseBoolean(Element element) {
        return parseBoolean(element.text());
    }
}
