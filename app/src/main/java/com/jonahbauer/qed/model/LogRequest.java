package com.jonahbauer.qed.model;

import static com.jonahbauer.qed.model.LogRequest.Mode.*;

import android.content.res.Resources;
import android.net.Uri;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.networking.NetworkConstants;
import com.jonahbauer.qed.util.TimeUtils;

import org.jetbrains.annotations.Contract;

import java.io.Serializable;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
@Keep
public abstract class LogRequest implements Serializable {
    private final Mode mode;

    protected LogRequest(Mode mode) {
        this.mode = mode;
    }

    public abstract String getQueryString();

    public abstract String getSubtitle(Resources resources);

    @Nullable
    @Contract("null -> null")
    public static LogRequest parse(@Nullable Uri uri) {
        if (uri == null) return null;

        String mode = uri.getQueryParameter("mode");
        if (mode == null) return null;

        String channel = uri.getQueryParameter("channel");
        if (channel == null) channel = "";

        try {
            switch (mode) {
                case "postrecent": {
                    long last = Long.parseLong(uri.getQueryParameter("last"));
                    return new PostRecentLogRequest(channel, last);
                }
                case "daterecent": {
                    long seconds = Long.parseLong(uri.getQueryParameter("last"));
                    return new DateRecentLogRequest(channel, seconds, TimeUnit.SECONDS);
                }
                case "postinterval": {
                    long from = Long.parseLong(uri.getQueryParameter("from"));
                    long to = Long.parseLong(uri.getQueryParameter("to"));
                    return new PostIntervalLogRequest(channel, from, to);
                }
                case "dateinterval": {
                    LocalDate localFrom = LocalDate.from(DateIntervalLogRequest.DATE_PARSER.parse(uri.getQueryParameter("from")));
                    LocalDate localTo = LocalDate.from(DateIntervalLogRequest.DATE_PARSER.parse(uri.getQueryParameter("to")));

                    Instant from = ZonedDateTime.of(localFrom, LocalTime.MIN, NetworkConstants.SERVER_TIME_ZONE).toInstant();
                    Instant to = ZonedDateTime.of(localTo, LocalTime.MAX, NetworkConstants.SERVER_TIME_ZONE).toInstant();
                    return new DateIntervalLogRequest(channel, from, to);
                }
                case "fromownpost": {
                    long skip = Long.parseLong(uri.getQueryParameter("skip"));
                    return new SinceOwnLogRequest(channel, skip);
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    public enum Mode {
        POST_RECENT,
        DATE_RECENT,
        DATE_INTERVAL,
        POST_INTERVAL,
        SINCE_OWN,
        FILE;

        @StringRes
        public int toStringRes() {
            switch (this) {
                case POST_RECENT: return R.string.log_request_mode_postrecent;
                case DATE_RECENT: return R.string.log_request_mode_daterecent;
                case DATE_INTERVAL: return R.string.log_request_mode_dateinterval;
                case POST_INTERVAL: return R.string.log_request_mode_postinterval;
                case SINCE_OWN: return R.string.log_request_mode_sinceown;
                case FILE: return R.string.log_request_mode_file;
            }
            throw new AssertionError();
        }
    }

    @Getter
    @EqualsAndHashCode(callSuper = true)
    public static abstract class OnlineLogRequest extends LogRequest {
        private final String channel;

        protected OnlineLogRequest(Mode mode, String channel) {
            super(mode);
            this.channel = channel;
        }

        @Override
        public String getQueryString() {
            return "?channel=" + channel;
        }
    }

    /**
     * A {@link LogRequest} requesting the {@link #last n} most recent posts.
     */
    @Getter
    @EqualsAndHashCode(callSuper = true)
    public static class PostRecentLogRequest extends OnlineLogRequest {
        private final long last;

        public PostRecentLogRequest(String channel, long last) {
            super(POST_RECENT, channel);
            this.last = last;
        }

        @Override
        public String getQueryString() {
            return super.getQueryString() + "&mode=postrecent&last=" + last;
        }

        @Override
        public String getSubtitle(Resources resources) {
            return MessageFormat.format(resources.getString(R.string.log_subtitle_post_recent), last);
        }
    }

    /**
     * A {@link LogRequest} requesting all posts since {@link #seconds n} seconds ago.
     */
    @Getter
    @EqualsAndHashCode(callSuper = true)
    public static class DateRecentLogRequest extends OnlineLogRequest {
        private final long seconds;

        public DateRecentLogRequest(String channel, long duration, TimeUnit timeUnit) {
            super(DATE_RECENT, channel);
            this.seconds = timeUnit.toSeconds(duration);
        }

        @Override
        public String getQueryString() {
            return super.getQueryString() + "&mode=daterecent&last=" + seconds;
        }

        @Override
        public String getSubtitle(Resources resources) {
            return MessageFormat.format(resources.getString(R.string.log_subtitle_date_recent), seconds / 3600);
        }
    }

    /**
     * A {@link LogRequest} requesting all posts that have an {@link Message#getId() id} between
     * {@link #from} and {@link #to} (both inclusive).
     */
    @Getter
    @EqualsAndHashCode(callSuper = true)
    public static class PostIntervalLogRequest extends OnlineLogRequest {
        private final long from;
        private final long to;

        public PostIntervalLogRequest(String channel, long from, long to) {
            super(POST_INTERVAL, channel);
            this.from = from;
            this.to = to;
        }

        @Override
        public String getQueryString() {
            return super.getQueryString() + "&mode=postinterval&from=" + from + "&to=" + to;
        }

        @Override
        public String getSubtitle(Resources resources) {
            return MessageFormat.format(resources.getString(R.string.log_subtitle_post_interval), from, to);
        }
    }

    /**
     * A {@link LogRequest} requesting all posts posted between {@link #from} and {@link #to}.
     */
    @Getter
    @EqualsAndHashCode(callSuper = true)
    public static class DateIntervalLogRequest extends OnlineLogRequest {
        private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
                .ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                .withZone(NetworkConstants.SERVER_TIME_ZONE);
        private static final DateTimeFormatter DATE_PARSER = DateTimeFormatter
                .ofPattern("yyyy-MM-dd");

        private final Instant from;
        private final Instant to;

        public DateIntervalLogRequest(String channel, Instant from, Instant to) {
            super(DATE_INTERVAL, channel);
            this.from = from;
            this.to = to;
        }

        @Override
        public String getQueryString() {
            return super.getQueryString() + "&mode=dateinterval&from=" + DATE_TIME_FORMATTER.format(from) + "&to=" + DATE_TIME_FORMATTER.format(to);
        }

        @Override
        public String getSubtitle(Resources resources) {
            return MessageFormat.format(resources.getString(R.string.log_subtitle_date_interval), TimeUtils.format(from), TimeUtils.format(to));
        }
    }

    /**
     * A {@link LogRequest} requesting all posts since one's most recent post skipping the {@link #skip n}
     * most recent posts.
     */
    @Getter
    @EqualsAndHashCode(callSuper = true)
    public static class SinceOwnLogRequest extends OnlineLogRequest {
        private final long skip;

        public SinceOwnLogRequest(String channel, long skip) {
            super(SINCE_OWN, channel);
            this.skip = skip;
        }

        @Override
        public String getQueryString() {
            return super.getQueryString() + "&mode=fromownpost&skip=" + skip;
        }

        @Override
        public String getSubtitle(Resources resources) {
            return resources.getString(R.string.log_subtitle_since_own);
        }
    }

    @Getter
    @EqualsAndHashCode(callSuper = true)
    public static class FileLogRequest extends LogRequest {
        private final Uri file;

        public FileLogRequest(Uri file) {
            super(FILE);
            this.file = file;
        }

        @Override
        public String getQueryString() {
            return "file://" + file.toString();
        }

        @Override
        public String getSubtitle(Resources resources) {
            return resources.getString(R.string.log_subtitle_file);
        }
    }
}
