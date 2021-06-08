package com.jonahbauer.qed.networking.parser;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Contract;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public abstract class HtmlParser<T> implements Parser<T> {
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY);

    @Override
    public final T apply(T obj, String s) {
        Document document = Jsoup.parse(s);
        return parse(obj, document);
    }

    @NonNull
    @Contract("!null, _ -> param1")
    protected abstract T parse(@NonNull T obj, Document document) throws HtmlParseException;

    protected Date parseDate(String date) {
        try {
            return SIMPLE_DATE_FORMAT.parse(date);
        } catch (ParseException e) {
            return null;
        }
    }
}
