package com.jonahbauer.qed.model.viewmodel;

import android.app.Application;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.model.Message;
import com.jonahbauer.qed.networking.QEDChatPages;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.networking.async.QEDPageStreamReceiver;
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

import lombok.EqualsAndHashCode;
import lombok.Getter;

import static com.jonahbauer.qed.model.viewmodel.LogViewModel.Mode.DATE_INTERVAL;
import static com.jonahbauer.qed.model.viewmodel.LogViewModel.Mode.DATE_RECENT;
import static com.jonahbauer.qed.model.viewmodel.LogViewModel.Mode.FILE;
import static com.jonahbauer.qed.model.viewmodel.LogViewModel.Mode.POST_INTERVAL;
import static com.jonahbauer.qed.model.viewmodel.LogViewModel.Mode.POST_RECENT;
import static com.jonahbauer.qed.model.viewmodel.LogViewModel.Mode.SINCE_OWN;

public class LogViewModel extends AndroidViewModel {
    private final MutableLiveData<StatusWrapper<List<Message>>> mMessages = new MutableLiveData<>();
    private final MutableLiveData<Pair<Long, Long>> mDownloadStatus = new MutableLiveData<>();
    private final MutableLiveData<Pair<Long, Long>> mParseStatus = new MutableLiveData<>();

    private AsyncTask<?,?,?> mRunningTask;

    public LogViewModel(@NonNull Application application) {
        super(application);
    }

    public void load(@NonNull LogRequest logRequest) {
        mMessages.setValue(StatusWrapper.wrap(Collections.emptyList(), StatusWrapper.STATUS_PRELOADED));
        mDownloadStatus.setValue(null);
        mParseStatus.setValue(null);

        if (mRunningTask != null) {
            mRunningTask.cancel(false);
            mRunningTask = null;
        }

        if (logRequest instanceof FileLogRequest) {
            parse(((FileLogRequest) logRequest).getFile());
        } else {
            try {
                File tempDir = getApplication().getCacheDir();
                File file = File.createTempFile("chat", ".log", tempDir);
                file.deleteOnExit();

                mRunningTask = QEDChatPages.getChatLog(getApplication(), logRequest, Uri.fromFile(file), new DownloadListener());
            } catch (IOException e) {
                onError(Reason.guess(e));
            }
        }
    }

    public LiveData<StatusWrapper<List<Message>>> getMessages() {
        return mMessages;
    }

    public LiveData<Pair<Long, Long>> getDownloadStatus() {
        return mDownloadStatus;
    }

    public LiveData<Pair<Long, Long>> getParseStatus() {
        return mParseStatus;
    }

    private void parse(Uri file) {
        if (mRunningTask != null) {
            mRunningTask.cancel(false);
        }
        mParseStatus.setValue(null);
        mRunningTask = QEDChatPages.parseChatLog(getApplication(), file, new ParseListener());
    }

    private void onError(@NonNull Reason reason) {
        mMessages.setValue(StatusWrapper.wrap(Collections.emptyList(), reason));
    }

    @Override
    protected void onCleared() {
        if (mRunningTask != null) {
            mRunningTask.cancel(false);
        }
    }

    private class DownloadListener implements QEDPageStreamReceiver<Uri> {

        @Override
        public void onPageReceived(@NonNull Uri out) {
            parse(out);
        }

        @Override
        public void onError(Uri out, @NonNull Reason reason, @Nullable Throwable cause) {
            QEDPageStreamReceiver.super.onError(out, reason, cause);
            LogViewModel.this.onError(reason);
        }

        @Override
        public void onProgressUpdate(Uri obj, long done, long total) {
            mDownloadStatus.setValue(Pair.create(done, total));
        }
    }

    private class ParseListener implements QEDPageStreamReceiver<List<Message>> {

        @Override
        public void onPageReceived(@NonNull List<Message> out) {
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
            mParseStatus.setValue(Pair.create(done, total));
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
    }

    @Getter
    @EqualsAndHashCode(callSuper = true)
    public static abstract class OnlineLogRequest extends LogRequest {
        private final String channel;

        protected OnlineLogRequest(Mode mode, String channel) {
            super(mode);
            this.channel = channel;
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
            return "?mode=postrecent&last=" + last;
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
            return "?mode=daterecent&last=" + seconds;
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
            return "?mode=postinterval&from=" + from + "&to=" + to;
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
            return "?mode=dateinterval&from=" + DATE_FORMAT.format(from) + "&to=" + DATE_FORMAT.format(to);
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
            return "?mode=fromownpost&skip=" + skip;
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
