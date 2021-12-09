package com.jonahbauer.qed.util;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.jonahbauer.qed.networking.NetworkConstants;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TimeUtils {
    public static final DateTimeFormatter DATE_MEDIUM = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
    public static final DateTimeFormatter TIME_MEDIUM = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM);
    public static final DateTimeFormatter DATE_TIME_MEDIUM = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);

    public static String format(LocalDate date) {
        if (date == null) return null;
        return DATE_MEDIUM.format(date);
    }

    public static String format(LocalTime time) {
        if (time == null) return null;
        return TIME_MEDIUM.format(time);
    }

    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return DATE_TIME_MEDIUM.format(dateTime);
    }

    public static String format(Instant instant) {
        if (instant == null) return null;
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                                .withZone(ZoneId.systemDefault())
                                .format(instant);
    }

    /**
     * Simulates what would happen to the give {@link LocalDateTime} after being sent to and
     * processed by the server and then converted back to the users local time.
     */
    public static LocalDateTime check(LocalDateTime dateTime) {
        Instant instant = ZonedDateTime.of(dateTime, ZoneId.systemDefault()).toInstant();
        LocalDateTime serverLocalTime = ZonedDateTime.ofInstant(instant, NetworkConstants.SERVER_TIME_ZONE).toLocalDateTime();
        Instant serverZonedTime = ZonedDateTime.of(serverLocalTime, NetworkConstants.SERVER_TIME_ZONE).toInstant();
        return LocalDateTime.ofInstant(serverZonedTime, ZoneId.systemDefault());
    }

    public static LiveData<LocalDateTime> combine(LiveData<LocalDate> date, LiveData<LocalTime> time) {
        MediatorLiveData<LocalDateTime> out = new MediatorLiveData<>();
        out.addSource(date, d -> out.setValue(LocalDateTime.of(date.getValue(), time.getValue())));
        out.addSource(time, t -> out.setValue(LocalDateTime.of(date.getValue(), time.getValue())));
        return out;
    }
}