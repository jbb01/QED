package eu.jonahbauer.qed.network.parser;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import eu.jonahbauer.qed.model.util.ParsedInstant;
import eu.jonahbauer.qed.model.util.ParsedLocalDate;
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
    protected abstract T parse(@NonNull T obj, @NonNull Document document) throws HtmlParseException;

    @Nullable
    protected static LocalDate parseLocalDate(@NonNull String date) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            Log.w(LOG_TAG, "Could not parse local date \"" + date + "\".", e);
            return null;
        }
    }

    @NonNull
    protected static ParsedLocalDate parseLocalDate(@NonNull Element time) {
        return new ParsedLocalDate(time.text(), parseLocalDate(time.attr("datetime")));
    }

    @Nullable
    protected static Instant parseInstant(@NonNull String instant) {
        try {
            return OffsetDateTime.parse(instant).toInstant();
        } catch (DateTimeParseException e) {
            Log.w(LOG_TAG, "Could not parse instant \"" + instant + "\".", e);
            return null;
        }
    }

    @NonNull
    protected static ParsedInstant parseInstant(@NonNull Element time) {
        return new ParsedInstant(time.text(), parseInstant(time.attr("datetime")));
    }

    @Nullable
    protected static Boolean parseBoolean(@Nullable String bool) {
        if ("Ja".equalsIgnoreCase(bool)) {
            return true;
        } else if ("Nein".equalsIgnoreCase(bool)) {
            return false;
        } else {
            Log.w(LOG_TAG, "Could not parse boolean \"" + bool + "\".");
            return null;
        }
    }
}
