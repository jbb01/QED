package com.jonahbauer.qed.networking.parser;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Contract;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public abstract class HtmlParser<T> implements Parser<T> {
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = new ThreadLocal<>() {
        @NonNull
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY);
            sdf.setTimeZone(TimeZone.getTimeZone("CET"));
            return sdf;
        }
    };

    @Override
    public final T apply(T obj, String s) {
        Document document = Jsoup.parse(s);
        return parse(obj, document);
    }

    @NonNull
    @Contract("_, _ -> param1")
    protected abstract T parse(@NonNull T obj, Document document) throws HtmlParseException;

    protected Date parseDate(String date) {
        try {
            //noinspection ConstantConditions
            return DATE_FORMAT.get().parse(date);
        } catch (ParseException e) {
            return null;
        }
    }
}
