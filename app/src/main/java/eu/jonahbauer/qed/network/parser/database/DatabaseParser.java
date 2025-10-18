package eu.jonahbauer.qed.network.parser.database;

import android.net.Uri;
import android.util.Log;
import android.util.Pair;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import eu.jonahbauer.qed.model.Person;
import eu.jonahbauer.qed.model.Registration;
import eu.jonahbauer.qed.model.util.ParsedInstant;
import eu.jonahbauer.qed.model.util.ParsedLocalDate;
import eu.jonahbauer.qed.network.parser.HtmlParseException;
import eu.jonahbauer.qed.network.parser.HtmlParser;
import eu.jonahbauer.qed.util.TextUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class DatabaseParser<T> extends HtmlParser<T> {
    private static final String LOG_TAG = DatabaseParser.class.getName();

    private static final Pattern COST_PATTERN = Pattern.compile("((?:\\d+\\.)*\\d+(?:,\\d+)?)\\s*€");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");

    private static final String GENDER_MALE = "männlich";
    private static final String GENDER_FEMALE = "weiblich";
    private static final String GENDER_OTHER = "andere";

    private static final String STATUS_REJECTED = "abgelehnt";
    private static final String STATUS_CANCELLED = "abgesagt";

    private static final String REGISTRATIONS_KEY_ORGA = "Orga";

    protected static long parseIdFromHref(@Nullable Element element, long defaultValue) {
        var id = parseIdFromHref(element);
        return id != null ? id : defaultValue;
    }

    @Nullable
    protected static Long parseIdFromHref(@Nullable Element element) {
        if (element == null) return null;
        try {
            if (element.hasAttr("href")) {
                var href = Uri.parse(element.attr("href"));
                return Long.parseLong(href.getLastPathSegment());
            } else {
                var e = element.selectFirst("[href]");
                if (e == null) return null;

                var href = Uri.parse(e.attr("href"));
                return Long.parseLong(href.getLastPathSegment());
            }
        } catch (Exception e) {
            return null;
        }
    }

    protected static void parseDefinitionList(
            @Nullable Element element,
            @NonNull BiConsumer<Element, Element> action
    ) {
        parseDefinitionList(element, true, action);
    }

    protected static void parseDefinitionList(
            @Nullable Element element, boolean filtered,
            @NonNull BiConsumer<Element, Element> action
    ) {
        if (element == null) return;

        for (var dt : element.select("dt")) {
            try {
                var dd = dt.nextElementSibling();
                if (dd == null || !dd.nameIs("dd")) {
                    throw new HtmlParseException("missing <dd> element after <dt>");
                }

                if (filtered && dd.selectFirst("> i") != null) {
                    continue;
                }

                action.accept(dt, dd);
            } catch (Exception e) {
                var document = element.ownerDocument();
                var location = document == null || document.location().isEmpty() ? "unknown" : document.location();
                Log.e(LOG_TAG, "Error parsing document at " + location, e);
            }
        }
    }

    protected static void parseDefinitionList(
            @Nullable Element element,
            @NonNull Map<String, BiConsumer<Element, Element>> actions
    ) {
        parseDefinitionList(element, (dt, dd) -> {
            String key = dt.text();

            int bestMatchLength = 0;
            BiConsumer<Element, Element> bestMatchAction = null;
            for (var action : actions.entrySet()) {
                if (key.toLowerCase(Locale.ROOT).contains(action.getKey().toLowerCase(Locale.ROOT))) {
                    if (action.getKey().length() > bestMatchLength) {
                        bestMatchLength = action.getKey().length();
                        bestMatchAction = action.getValue();
                    }
                }
            }

            if (bestMatchAction != null) {
                bestMatchAction.accept(dt, dd);
            }
        });
    }




    protected static void parseLocalDate(
            @Nullable Element element,
            @NonNull Consumer<ParsedLocalDate> action
    ) {
        if (element == null) return;

        Element time = element.nameIs("time") ? element : element.selectFirst("time");
        if (time == null) return;

        try {
            action.accept(parseLocalDate(time));
        } catch (Exception ignored) {
            // ignored
        }
    }

    protected static void parseLocalDateRange(
            @Nullable Element element,
            @NonNull Consumer<Pair<ParsedLocalDate, ParsedLocalDate>> action
    ) {
        if (element == null) return;

        Elements time = element.select("time");
        try {
            if (time.size() == 1) {
                ParsedLocalDate date = parseLocalDate(time.get(0));
                action.accept(Pair.create(date, date));
            } else if (time.size() == 2) {
                ParsedLocalDate start = parseLocalDate(time.get(0));
                ParsedLocalDate end = parseLocalDate(time.get(1));
                action.accept(Pair.create(start, end));
            }
        } catch (Exception ignored) {
            // ignored
        }
    }

    protected static void parseInstant(
            @Nullable Element element,
            @NonNull Consumer<ParsedInstant> action
    ) {
        if (element == null) return;

        Element time = element.nameIs("time") ? element : element.selectFirst("time");
        if (time == null) return;

        try {
            action.accept(parseInstant(time));
        } catch (Exception ignored) {
            // ignored
        }
    }

    protected static void parseString(
            @Nullable Element element,
            @NonNull Consumer<String> action
    ) {
        if (element == null || element.selectFirst("> i") != null) return;

        try {
            action.accept(element.text());
        } catch (Exception ignored) {
            // ignored
        }
    }

    protected static void parseMultilineString(
            @Nullable Element element,
            @NonNull Consumer<String> action
    ) {
        if (element == null) return;

        try {
            var text = element.html().replaceAll("\\s*<br>\\s*", "\n").trim();
            if (TextUtils.isNullOrBlank(text)) return;
            action.accept(text);
        } catch (Exception ignored) {
            // ignored
        }
    }

    protected static void parseBoolean(
            @Nullable Element element,
            @NonNull Consumer<Boolean> action
    ) {
        if (element == null) return;

        try {
            action.accept(parseBoolean(element.text()));
        } catch (Exception ignored) {
            // ignored
        }
    }

    protected static void parseInteger(
            @Nullable Element element,
            @NonNull Consumer<Integer> action
    ) {
        if (element == null) return;

        try {
            Matcher matcher = NUMBER_PATTERN.matcher(element.text());
            if (matcher.find()) {
                action.accept(Integer.parseInt(matcher.group()));
            }
        } catch (Exception ignored) {
            // ignored
        }
    }

    protected static void parseCurrency(
            @Nullable Element element,
            @NonNull Consumer<Double> action
    ) {
        if (element == null) return;

        try {
            Matcher matcher = COST_PATTERN.matcher(element.text());
            if (matcher.find()) {
                String match = Objects.requireNonNull(matcher.group(1));
                match = match.replace(".", "");
                match = match.replace(',', '.');
                action.accept(Double.parseDouble(match));
            }
        } catch (Exception ignored) {
            // ignored
        }
    }

    protected static void parsePersonGender(
            @Nullable Element element,
            @NonNull Consumer<Person.Gender> action
    ) {
        if (element == null) return;

        try {
            switch (element.text()) {
                case GENDER_MALE: {
                    action.accept(Person.Gender.MALE);
                    break;
                }
                case GENDER_FEMALE: {
                    action.accept(Person.Gender.FEMALE);
                    break;
                }
                case GENDER_OTHER: {
                    action.accept(Person.Gender.OTHER);
                    break;
                }
            }
        } catch (Exception ignored) {
            // ignored
        }
    }

    protected static void parseRegistrationStatus(
            @Nullable Element element,
            @NonNull BiConsumer<Registration.Status, Boolean> action
    ) {
        if (element == null) return;

        try {
            String string = element.text();
            boolean isOrga = string.contains(REGISTRATIONS_KEY_ORGA);

            if (string.contains(STATUS_REJECTED)) {
                action.accept(Registration.Status.REJECTED, isOrga);
            } else if (string.contains(STATUS_CANCELLED)) {
                action.accept(Registration.Status.CANCELLED, isOrga);
            } else {
                action.accept(Registration.Status.UNKNOWN, isOrga);
            }
        } catch (Exception ignored) {
            // ignored
        }
    }
}
