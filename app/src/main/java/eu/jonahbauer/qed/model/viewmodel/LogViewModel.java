package eu.jonahbauer.qed.model.viewmodel;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import eu.jonahbauer.qed.model.LogRequest;
import eu.jonahbauer.qed.model.LogRequest.FileLogRequest;
import eu.jonahbauer.qed.model.Message;
import eu.jonahbauer.qed.ui.adapter.MessageAdapter;
import eu.jonahbauer.qed.networking.Reason;
import eu.jonahbauer.qed.networking.async.QEDPageStreamReceiver;
import eu.jonahbauer.qed.networking.pages.QEDChatPages;
import eu.jonahbauer.qed.util.StatusWrapper;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import it.unimi.dsi.fastutil.longs.LongLongImmutablePair;

public class LogViewModel extends AndroidViewModel {
    private final MutableLiveData<StatusWrapper<List<Message>>> mMessages = new MutableLiveData<>();
    private final MutableLiveData<LongLongImmutablePair> mDownloadStatus = new MutableLiveData<>();
    private final MutableLiveData<LongLongImmutablePair> mParseStatus = new MutableLiveData<>();

    private final MutableLiveData<LogRequest> mLogRequest = new MutableLiveData<>();

    private final CompositeDisposable mDisposable = new CompositeDisposable();

    private int mCheckedItemPosition = MessageAdapter.INVALID_POSITION;

    public LogViewModel(@NonNull Application application) {
        super(application);
        mLogRequest.observeForever(logRequest -> {
            mDisposable.clear();
            mMessages.setValue(StatusWrapper.preloaded(Collections.emptyList()));
            mDownloadStatus.setValue(null);
            mParseStatus.setValue(null);

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
        });
    }

    public void load(@NonNull LogRequest logRequest) {
        if (!Objects.equals(mLogRequest.getValue(), logRequest)) {
            mCheckedItemPosition = MessageAdapter.INVALID_POSITION;
            mLogRequest.setValue(logRequest);
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

    public LiveData<LogRequest> getLogRequest() {
        return mLogRequest;
    }

    public void setCheckedItemPosition(int checkedItemPosition) {
        this.mCheckedItemPosition = checkedItemPosition;
    }

    public int getCheckedItemPosition() {
        return mCheckedItemPosition;
    }

    private void parse(Uri file) {
        mDisposable.clear();
        mParseStatus.setValue(null);
        mDisposable.add(
                QEDChatPages.parseChatLog(getApplication(), file, new ParseListener())
        );
    }

    private void onError(@NonNull Reason reason) {
        mMessages.setValue(StatusWrapper.error(Collections.emptyList(), reason));
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
                mMessages.setValue(StatusWrapper.loaded(out));
            } else {
                mMessages.setValue(StatusWrapper.error(Collections.emptyList(), Reason.EMPTY));
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
}
