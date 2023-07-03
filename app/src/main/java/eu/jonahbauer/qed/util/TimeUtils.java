package eu.jonahbauer.qed.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import eu.jonahbauer.qed.model.util.ParsedInstant;
import eu.jonahbauer.qed.model.util.ParsedLocalDate;
import eu.jonahbauer.qed.networking.NetworkConstants;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.function.Function;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TimeUtils {
    public static final DateTimeFormatter DATE_MEDIUM = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
    public static final DateTimeFormatter TIME_MEDIUM = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM);
    public static final DateTimeFormatter DATE_TIME_MEDIUM = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
    public static final Function<LocalDate, String> LOCAL_DATE_FORMATTER = TimeUtils::format;
    public static final Function<ParsedLocalDate, String> PARSED_LOCAL_DATE_FORMATTER = TimeUtils::format;

    /**
     * Formats the given {@link ParsedLocalDate} using {@link #format(LocalDate)} defaulting to the
     * {@linkplain ParsedLocalDate#getString() original value} when the date could not be parsed.
     * @see #format(LocalDate)
     */
    public static String format(ParsedLocalDate parsedDate) {
        if (parsedDate == null) return null;

        var date = parsedDate.getLocalDate();
        if (date != null) return format(date);
        else return parsedDate.getString();
    }

    /**
     * Formats the given {@link LocalDate} using {@link FormatStyle#MEDIUM}.
     */
    public static String format(LocalDate date) {
        if (date == null) return null;
        return DATE_MEDIUM.format(date);
    }

    /**
     * Formats the given {@link LocalTime} using {@link FormatStyle#MEDIUM}.
     */
    public static String format(LocalTime time) {
        if (time == null) return null;
        return TIME_MEDIUM.format(time);
    }

    /**
     * Formats the given {@link  LocalDateTime} using {@link FormatStyle#MEDIUM}.
     */
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return DATE_TIME_MEDIUM.format(dateTime);
    }

    /**
     * Formats the given {@link ParsedInstant} using {@link #format(Instant)} defaulting to the
     * {@linkplain  ParsedInstant#getString() original value} when the instant could not be parsed.
     * @see #format(Instant)
     */
    public static String format(ParsedInstant parsedInstant) {
        if (parsedInstant == null) return null;

        var instant = parsedInstant.getInstant();
        if (instant != null) return format(instant);
        else return parsedInstant.getString();
    }

    /**
     * Formats the given {@link Instant} as a localized date time in the
     * {@linkplain ZoneId#systemDefault() default time zone} using {@link FormatStyle#MEDIUM}.
     */
    public static String format(Instant instant) {
        if (instant == null) return null;
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                                .withZone(ZoneId.systemDefault())
                                .format(instant);
    }

    /**
     * Returns {@code true} when the given date is either {@code null} or its
     * {@linkplain #format(ParsedLocalDate) string representation} is {@linkplain TextUtils#isBlank(String) blank}.
     */
    public static boolean isNullOrBlank(@Nullable ParsedLocalDate parsedLocalDate) {
        return parsedLocalDate == null
                || parsedLocalDate.getLocalDate() == null && TextUtils.isNullOrBlank(parsedLocalDate.getString());
    }

    /**
     * Simulates what would happen to the give {@link LocalDateTime} after being sent to and
     * processed by the server and then converted back to the users local time.
     */
    public static @NonNull LocalDateTime check(@NonNull LocalDateTime dateTime) {
        Instant instant = ZonedDateTime.of(dateTime, ZoneId.systemDefault()).toInstant();
        LocalDateTime serverLocalTime = ZonedDateTime.ofInstant(instant, NetworkConstants.SERVER_TIME_ZONE).toLocalDateTime();
        Instant serverZonedTime = ZonedDateTime.of(serverLocalTime, NetworkConstants.SERVER_TIME_ZONE).toInstant();
        return LocalDateTime.ofInstant(serverZonedTime, ZoneId.systemDefault());
    }

    /**
     * Converts the given {@link LocalDateTime} to an {@link Instant} assuming the
     * {@linkplain ZoneId#systemDefault() default time zone}.
     */
    public static Instant instantFromLocal(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return ZonedDateTime.of(dateTime, ZoneId.systemDefault()).toInstant();
    }

    /**
     * Converts the given {@link Instant} to an {@link LocalDateTime} assuming the
     * {@linkplain ZoneId#systemDefault() default time zone}.
     */
    public LocalDateTime localFromInstant(Instant instant) {
        if (instant == null) return null;
        return ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalDateTime();
    }
}
