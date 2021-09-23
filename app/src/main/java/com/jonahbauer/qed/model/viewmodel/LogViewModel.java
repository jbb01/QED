package com.jonahbauer.qed.model.viewmodel;

import static com.jonahbauer.qed.model.viewmodel.LogViewModel.Mode.DATE_INTERVAL;
import static com.jonahbauer.qed.model.viewmodel.LogViewModel.Mode.DATE_RECENT;
import static com.jonahbauer.qed.model.viewmodel.LogViewModel.Mode.FILE;
import static com.jonahbauer.qed.model.viewmodel.LogViewModel.Mode.POST_INTERVAL;
import static com.jonahbauer.qed.model.viewmodel.LogViewModel.Mode.POST_RECENT;
import static com.jonahbauer.qed.model.viewmodel.LogViewModel.Mode.SINCE_OWN;

import android.app.Application;
import android.content.res.Resources;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.model.Message;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.networking.async.QEDPageStreamReceiver;
import com.jonahbauer.qed.networking.pages.QEDChatPages;
import com.jonahbauer.qed.util.StatusWrapper;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import it.unimi.dsi.fastutil.longs.LongLongImmutablePair;
import lombok.EqualsAndHashCode;
import lombok.Getter;

public class LogViewModel extends AndroidViewModel {
    private final MutableLiveData<StatusWrapper<List<Message>>> mMessages = new MutableLiveData<>();
    private final MutableLiveData<LongLongImmutablePair> mDownloadStatus = new MutableLiveData<>();
    private final MutableLiveData<LongLongImmutablePair> mParseStatus = new MutableLiveData<>();

    private final CompositeDisposable mDisposable = new CompositeDisposable();

    public LogViewModel(@NonNull Application application) {
        super(application);
    }

    public void load(@NonNull LogRequest logRequest) {
        mMessages.setValue(StatusWrapper.wrap(Collections.emptyList(), StatusWrapper.STATUS_PRELOADED));
        mDownloadStatus.setValue(null);
        mParseStatus.setValue(null);
        mDisposable.clear();

        try {
            if (logRequest instanceof FileLogRequest) {
                Uri file = ((FileLogRequest) logRequest).getFile();

                // set download status
                var descriptor = getApplication().getContentResolver().openAssetFileDescriptor(file, "r");
                if (descriptor == null) throw new IOException("Content resolver returned null.");
                long length = descriptor.getLength();
                if (length == -1) length = 0;
                mDownloadStatus.setValue(LongLongImmutablePair.of(length, length));

                parse(file);
            } else {
                File tempDir = getApplication().getCacheDir();
                File file = File.createTempFile("chat", ".log", tempDir);
                file.deleteOnExit();

                mDisposable.add(
                        QEDChatPages.getChatLog(getApplication(), logRequest, Uri.fromFile(file), new DownloadListener())
                );
            }
        } catch (IOException e) {
            onError(Reason.guess(e));
        }
    }

    public LiveData<StatusWrapper<List<Message>>> getMessages() {
        return mMessages;
    }

    public LiveData<LongLongImmutablePair> getDownloadStatus() {
        return mDownloadStatus;
    }

    public LiveData<LongLongImmutablePair> getParseStatus() {
        return mParseStatus;
    }

    private void parse(Uri file) {
        mDisposable.clear();
        mParseStatus.setValue(null);
        mDisposable.add(
                QEDChatPages.parseChatLog(getApplication(), file, new ParseListener())
        );
    }

    private void onError(@NonNull Reason reason) {
        mMessages.setValue(StatusWrapper.wrap(Collections.emptyList(), reason));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mDisposable.clear();
    }

    private class DownloadListener implements QEDPageStreamReceiver<Uri> {
        private long size;

        @Override
        public void onResult(@NonNull Uri out) {
            mDownloadStatus.setValue(LongLongImmutablePair.of(size, size));
            parse(out);
        }

        @Override
        public void onError(Uri out, @NonNull Reason reason, @Nullable Throwable cause) {
            QEDPageStreamReceiver.super.onError(out, reason, cause);
            LogViewModel.this.onError(reason);
        }

        @Override
        public void onProgressUpdate(Uri obj, long done, long total) {
            this.size = done;
            mDownloadStatus.setValue(LongLongImmutablePair.of(done, total));
        }
    }

    private class ParseListener implements QEDPageStreamReceiver<List<Message>> {

        @Override
        public void onResult(@NonNull List<Message> out) {
            if (out.size() > 0) {
                mMessages.setValue(StatusWrapper.wrap(out, StatusWrapper.STATUS_LOADED));
            } else {
                mMessages.setValue(StatusWrapper.wrap(Collections.emptyList(), Reason.EMPTY));
            }
        }

        @Override
        public void onError(List<Message> out, @NonNull Reason reason, @Nullable Throwable cause) {
            QEDPageStreamReceiver.super.onError(out, reason, cause);
            LogViewModel.this.onError(reason);
        }

        @Override
        public void onProgressUpdate(List<Message> obj, long done, long total) {
            mParseStatus.setValue(LongLongImmutablePair.of(done, total));
        }
    }

    public enum Mode {
        POST_RECENT("postrecent"),
        DATE_RECENT("daterecent"),
        DATE_INTERVAL("dateinterval"),
        POST_INTERVAL("postinterval"),
        SINCE_OWN("fromownpost"),
        FILE("file");

        final String modeStr;

        Mode(String modeStr) {
            this.modeStr = modeStr;
        }
    }

    @Getter
    @EqualsAndHashCode
    public static abstract class LogRequest implements Serializable {
        private final Mode mode;

        protected LogRequest(Mode mode) {
            this.mode = mode;
        }

        public abstract String getQueryString();

        public abstract String getSubtitle(Resources resources);

        @Nullable
        @SuppressWarnings("ConstantConditions")
        public static LogRequest parse(@NonNull Uri uri) {
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
                        Date from = DateIntervalLogRequest.DATE_FORMAT.parse(uri.getQueryParameter("from"));
                        Date to = DateIntervalLogRequest.DATE_FORMAT.parse(uri.getQueryParameter("to"));
                        return new DateIntervalLogRequest(channel, from, to);
                    }
                    case "fromownpost": {
                        long skip = Long.parseLong(uri.getQueryParameter("skip"));
                        return new SinceOwnLogRequest(channel, skip);
                    }
                }
            } catch (Exception ignored) {}

            return null;
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

    @Getter
    @EqualsAndHashCode(callSuper = true)
    public static class DateIntervalLogRequest extends OnlineLogRequest {
        private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY);

        private final Date from;
        private final Date to;

        public DateIntervalLogRequest(String channel, Date from, Date to) {
            super(DATE_INTERVAL, channel);
            this.from = from;
            this.to = to;
        }

        @Override
        public String getQueryString() {
            return super.getQueryString() + "&mode=dateinterval&from=" + DATE_FORMAT.format(from) + "&to=" + DATE_FORMAT.format(to);
        }

        @Override
        public String getSubtitle(Resources resources) {
            return MessageFormat.format(resources.getString(R.string.log_subtitle_date_interval), from, to);
        }
    }

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
