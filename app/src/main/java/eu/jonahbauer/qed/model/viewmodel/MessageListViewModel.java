package eu.jonahbauer.qed.model.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import androidx.lifecycle.SavedStateHandle;
import eu.jonahbauer.qed.model.Message;
import eu.jonahbauer.qed.model.MessageFilter;
import eu.jonahbauer.qed.ui.adapter.MessageAdapter;
import eu.jonahbauer.qed.model.room.Database;
import eu.jonahbauer.qed.model.room.MessageDao;
import eu.jonahbauer.qed.util.Preferences;
import eu.jonahbauer.qed.util.StatusWrapper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MessageListViewModel extends AndroidViewModel {
    private static final String SAVED_FILTER = "message_filter";
    private static final String SAVED_EXPANDED = "message_filter_expanded";
    private static final String SAVED_DATETIME_FROM = "message_filter_datetime_from";
    private static final String SAVED_DATETIME_TO = "message_filter_datetime_to";

    private final MessageDao mMessageDao;
    private final MutableLiveData<MessageFilter> mFilter;
    private final MutableLiveData<StatusWrapper<List<Message>>> mMessages = new MutableLiveData<>();
    private final CompositeDisposable mDisposable = new CompositeDisposable();

    private int mCheckedItemPosition = MessageAdapter.INVALID_POSITION;

    // saved view state
    private final MutableLiveData<Boolean> mExpanded;
    private final MutableLiveData<LocalDateTime> mDateTimeFrom;
    private final MutableLiveData<LocalDateTime> mDateTimeTo;

    public MessageListViewModel(@NonNull Application application, @NonNull SavedStateHandle savedStateHandle) {
        super(application);

        mFilter = savedStateHandle.getLiveData(SAVED_FILTER, null);
        mExpanded = savedStateHandle.getLiveData(SAVED_EXPANDED, false);
        mDateTimeFrom = savedStateHandle.getLiveData(SAVED_DATETIME_FROM, LocalDateTime.now());
        mDateTimeTo = savedStateHandle.getLiveData(SAVED_DATETIME_TO, LocalDateTime.now());

        mMessageDao = Database.getInstance(application).messageDao();
        mFilter.observeForever(filter -> {
            mCheckedItemPosition = MessageAdapter.INVALID_POSITION;
            mDisposable.clear();
            if (filter == null) {
                mMessages.setValue(StatusWrapper.loaded(Collections.emptyList()));
            } else {
                mMessages.setValue(StatusWrapper.preloaded(Collections.emptyList()));
                var disposable = filter.search(mMessageDao, Preferences.getChat().getDbMaxResult())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                (messages) -> mMessages.setValue(StatusWrapper.loaded(messages)),
                                (e) -> mMessages.setValue(StatusWrapper.error(Collections.emptyList(), e))
                        );
                mDisposable.add(disposable);
            }
        });

        mMessages.setValue(StatusWrapper.loaded(Collections.emptyList()));
    }

    public void load(MessageFilter filter) {
        mFilter.setValue(filter);
    }

    public int getCheckedItemPosition() {
        return mCheckedItemPosition;
    }

    public void setCheckedItemPosition(int checkedItemPosition) {
        this.mCheckedItemPosition = checkedItemPosition;
    }

    public LiveData<StatusWrapper<List<Message>>> getMessages() {
        return mMessages;
    }

    public MutableLiveData<Boolean> getExpanded() {
        return mExpanded;
    }

    public MutableLiveData<LocalDateTime> getDateTimeFrom() {
        return mDateTimeFrom;
    }

    public MutableLiveData<LocalDateTime> getDateTimeTo() {
        return mDateTimeTo;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mDisposable.clear();
    }
}