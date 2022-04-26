package com.jonahbauer.qed.model.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.jonahbauer.qed.model.Message;
import com.jonahbauer.qed.model.MessageFilter;
import com.jonahbauer.qed.model.adapter.MessageAdapter;
import com.jonahbauer.qed.model.room.Database;
import com.jonahbauer.qed.model.room.MessageDao;
import com.jonahbauer.qed.util.Preferences;
import com.jonahbauer.qed.util.StatusWrapper;

import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MessageListViewModel extends AndroidViewModel {
    private final MessageDao mMessageDao;
    private final MutableLiveData<MessageFilter> mFilter = new MutableLiveData<>(null);
    private final MutableLiveData<StatusWrapper<List<Message>>> mMessages = new MutableLiveData<>();
    private final CompositeDisposable mDisposable = new CompositeDisposable();

    private int mCheckedItemPosition = MessageAdapter.INVALID_POSITION;

    public MessageListViewModel(@NonNull Application application) {
        super(application);

        mMessageDao = Database.getInstance(application).messageDao();
        mFilter.observeForever(filter -> {
            mCheckedItemPosition = MessageAdapter.INVALID_POSITION;
            mDisposable.clear();
            if (filter == null) {
                mMessages.setValue(StatusWrapper.loaded(Collections.emptyList()));
            } else {
                mMessages.setValue(StatusWrapper.preloaded(Collections.emptyList()));
                var disposable = filter.search(mMessageDao, Preferences.chat().getDbMaxResults())
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

    @Override
    protected void onCleared() {
        super.onCleared();
        mDisposable.clear();
    }
}