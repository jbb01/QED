package com.jonahbauer.qed.model;

import static com.jonahbauer.qed.model.LogRequest.Mode.DATE_INTERVAL;
import static com.jonahbauer.qed.model.LogRequest.Mode.DATE_RECENT;
import static com.jonahbauer.qed.model.LogRequest.Mode.FILE;
import static com.jonahbauer.qed.model.LogRequest.Mode.POST_INTERVAL;
import static com.jonahbauer.qed.model.LogRequest.Mode.POST_RECENT;
import static com.jonahbauer.qed.model.LogRequest.Mode.SINCE_OWN;

import android.content.res.Resources;
import android.net.Uri;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.*;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.model.parcel.ParcelExtensions;
import com.jonahbauer.qed.model.parcel.LambdaCreator;
import com.jonahbauer.qed.model.parcel.ParcelableEnum;
import com.jonahbauer.qed.networking.NetworkConstants;
import com.jonahbauer.qed.util.TimeUtils;

import lombok.RequiredArgsConstructor;
import lombok.experimental.ExtensionMethod;
import org.jetbrains.annotations.Contract;

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
@ExtensionMethod(ParcelExtensions.class)
public abstract class LogRequest implements Parcelable {
    private final @NonNull Mode mode;
    private final long timestamp;
    private final String query;

    public static final Creator<LogRequest> CREATOR = new LambdaCreator<>(LogRequest[]::new, source -> {
        var start = source.dataPosition();
        var mode = source.readTypedObject(Mode.CREATOR);
        source.setDataPosition(start);

        switch (mode) {
            case FILE: return new FileLogRequest(source);
            case SINCE_OWN: return new SinceOwnLogRequest(source);
            case DATE_RECENT: return new DateRecentLogRequest(source);
            case POST_RECENT: return new PostRecentLogRequest(source);
            case DATE_INTERVAL: return new DateIntervalLogRequest(source);
            case POST_INTERVAL: return new PostIntervalLogRequest(source);
            default: throw new AssertionError("Unknown LogRequest mode: " + mode);
        }
    });

    protected LogRequest(@NonNull Mode mode, String query) {
        this.mode = mode;
        this.timestamp = System.currentTimeMillis();
        this.query = "?mode=" + mode.getQuery() + query;
    }

    protected LogRequest(@NonNull Parcel parcel) {
        mode = parcel.readTypedObject(Mode.CREATOR);
        timestamp = parcel.readLong();
        query = parcel.readString();
    }

    public abstract String getSubtitle(@NonNull Resources resources);

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    @CallSuper
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedObject(mode, flags);
        dest.writeLong(timestamp);
        dest.writeString(query);
    }

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

    @Getter
    @RequiredArgsConstructor
    public enum Mode implements ParcelableEnum {
        POST_RECENT(R.string.log_request_mode_postrecent, "postrecent"),
        DATE_RECENT(R.string.log_request_mode_daterecent, "daterecent"),
        DATE_INTERVAL(R.string.log_request_mode_dateinterval, "dateinterval"),
        POST_INTERVAL(R.string.log_request_mode_postinterval, "postinterval"),
        SINCE_OWN(R.string.log_request_mode_sinceown, "fromownpost"),
        FILE(R.string.log_request_mode_file, "file");
        public static final Parcelable.Creator<Mode> CREATOR = new ParcelableEnum.Creator<>(Mode.values(), Mode[]::new);

        private final @StringRes int stringRes;
        private final String query;
    }

    @Getter
    @EqualsAndHashCode(callSuper = true)
    public static abstract class OnlineLogRequest extends LogRequest {
        private final @NonNull String channel;

        protected OnlineLogRequest(@NonNull Mode mode, @NonNull String channel, @NonNull String query) {
            super(mode, "&channel=" + channel + query);
            this.channel = channel;
        }

        protected OnlineLogRequest(@NonNull Parcel parcel) {
            super(parcel);
            this.channel = parcel.readString();
        }

        @Override
        @CallSuper
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(channel);
        }
    }

    /**
     * A {@link LogRequest} requesting the {@link #last n} most recent posts.
     */
    @Getter
    @EqualsAndHashCode(callSuper = true)
    public static class PostRecentLogRequest extends OnlineLogRequest {
        private final long last;

        public PostRecentLogRequest(@NonNull String channel, long last) {
            super(POST_RECENT, channel, "&last=" + last);
            this.last = last;
        }

        public PostRecentLogRequest(@NonNull Parcel parcel) {
            super(parcel);
            this.last = parcel.readLong();
        }

        @Override
        public String getSubtitle(@NonNull Resources resources) {
            return MessageFormat.format(resources.getString(R.string.log_subtitle_post_recent), last);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeLong(last);
        }
    }

    /**
     * A {@link LogRequest} requesting all posts since {@link #seconds n} seconds ago.
     */
    @Getter
    @EqualsAndHashCode(callSuper = true)
    public static class DateRecentLogRequest extends OnlineLogRequest {
        private final long seconds;

        public DateRecentLogRequest(@NonNull String channel, long duration, @NonNull TimeUnit timeUnit) {
            this(channel, timeUnit.toSeconds(duration));
        }

        public DateRecentLogRequest(@NonNull String channel, long seconds) {
            super(DATE_RECENT, channel, "&last=" + seconds);
            this.seconds = seconds;
        }

        public DateRecentLogRequest(@NonNull Parcel parcel) {
            super(parcel);
            this.seconds = parcel.readLong();
        }

        @Override
        public String getSubtitle(@NonNull Resources resources) {
            return MessageFormat.format(resources.getString(R.string.log_subtitle_date_recent), seconds / 3600);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeLong(seconds);
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

        public PostIntervalLogRequest(@NonNull String channel, long from, long to) {
            super(POST_INTERVAL, channel, "&from=" + from + "&to=" + to);
            this.from = from;
            this.to = to;
        }

        public PostIntervalLogRequest(@NonNull Parcel parcel) {
            super(parcel);
            this.from = parcel.readLong();
            this.to = parcel.readLong();
        }

        @Override
        public String getSubtitle(@NonNull Resources resources) {
            return MessageFormat.format(resources.getString(R.string.log_subtitle_post_interval), from, to);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeLong(from);
            dest.writeLong(to);
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

        private final @NonNull Instant from;
        private final @NonNull Instant to;

        public DateIntervalLogRequest(@NonNull String channel, @NonNull Instant from, @NonNull Instant to) {
            super(DATE_INTERVAL, channel, "&from=" + DATE_TIME_FORMATTER.format(from) + "&to=" + DATE_TIME_FORMATTER.format(to));
            this.from = from;
            this.to = to;
        }

        public DateIntervalLogRequest(@NonNull Parcel parcel) {
            super(parcel);
            this.from = parcel.readInstant();
            this.to = parcel.readInstant();
        }

        @Override
        public String getSubtitle(@NonNull Resources resources) {
            return MessageFormat.format(resources.getString(R.string.log_subtitle_date_interval), TimeUtils.format(from), TimeUtils.format(to));
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInstant(from);
            dest.writeInstant(to);
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

        public SinceOwnLogRequest(@NonNull String channel, long skip) {
            super(SINCE_OWN, channel, "&skip=" + skip);
            this.skip = skip;
        }

        public SinceOwnLogRequest(@NonNull Parcel parcel) {
            super(parcel);
            this.skip = parcel.readLong();
        }

        @Override
        public String getSubtitle(@NonNull Resources resources) {
            return resources.getString(R.string.log_subtitle_since_own);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeLong(skip);
        }
    }

    @Getter
    @EqualsAndHashCode(callSuper = true)
    public static class FileLogRequest extends LogRequest {
        private final @NonNull Uri file;
        private final @Nullable LogRequest root;

        public FileLogRequest(@NonNull Uri file) {
            this(file, null);
        }

        public FileLogRequest(@NonNull Uri file, @Nullable LogRequest root) {
            super(FILE, null);
            this.file = file;
            this.root = root;
        }

        public FileLogRequest(@NonNull Parcel parcel) {
            super(parcel);
            this.file = parcel.readTypedObject(Uri.CREATOR);
            this.root = parcel.readTypedObject(LogRequest.CREATOR);
        }

        @Override
        public String getQuery() {
            return "file://" + file;
        }

        @Override
        public String getSubtitle(@NonNull Resources resources) {
            if (root == null) {
                return resources.getString(R.string.log_subtitle_file);
            } else {
                return root.getSubtitle(resources);
            }
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeTypedObject(file, flags);
            dest.writeTypedObject(root, flags);
        }
    }
}
