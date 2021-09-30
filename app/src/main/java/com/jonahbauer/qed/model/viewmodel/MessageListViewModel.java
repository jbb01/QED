package com.jonahbauer.qed.model.viewmodel;

import static com.jonahbauer.qed.util.StatusWrapper.STATUS_LOADED;
import static com.jonahbauer.qed.util.StatusWrapper.STATUS_PRELOADED;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.jonahbauer.qed.model.Message;
import com.jonahbauer.qed.model.room.Database;
import com.jonahbauer.qed.model.room.MessageDao;
import com.jonahbauer.qed.util.StatusWrapper;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MessageListViewModel extends AndroidViewModel {
    private final MessageDao mMessageDao;
    private final MutableLiveData<StatusWrapper<List<Message>>> mMessages = new MutableLiveData<>();
    private final CompositeDisposable mDisposable = new CompositeDisposable();

    public MessageListViewModel(@NonNull Application application) {
        super(application);

        mMessageDao = Database.getInstance(application).messageDao();
        mMessages.setValue(StatusWrapper.wrap(Collections.emptyList(), STATUS_LOADED));
    }

    public void load(@Nullable String channel,
                     @Nullable String message,
                     @Nullable String name,
                     @Nullable Instant fromDate,
                     @Nullable Instant toDate,
                     @Nullable Long fromId,
                     @Nullable Long toId,
                     long limit) {
        mMessages.setValue(StatusWrapper.wrap(Collections.emptyList(), STATUS_PRELOADED));
        mDisposable.add(
                mMessageDao.findAll(channel, message, name, fromDate, toDate, fromId, toId, limit)
                           .subscribeOn(Schedulers.io())
                           .observeOn(AndroidSchedulers.mainThread())
                           .subscribe(
                                   (messages) -> mMessages.setValue(StatusWrapper.wrap(messages, STATUS_LOADED)),
                                   (e) -> mMessages.setValue(StatusWrapper.wrap(Collections.emptyList(), e))
                           )
        );
    }

    public LiveData<StatusWrapper<List<Message>>> getMessages() {
        return mMessages;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mDisposable.clear();
    }
}