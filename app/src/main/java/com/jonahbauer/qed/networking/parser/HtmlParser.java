package com.jonahbauer.qed.networking.parser;

import android.util.Log;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Contract;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.time.LocalDate;
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
}
