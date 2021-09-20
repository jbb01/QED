package com.jonahbauer.qed.model.viewmodel;

import static com.jonahbauer.qed.util.StatusWrapper.STATUS_LOADED;
import static com.jonahbauer.qed.util.StatusWrapper.STATUS_PRELOADED;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.jonahbauer.qed.model.Event;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.networking.async.QEDPageReceiver;
import com.jonahbauer.qed.networking.pages.QEDDBPages;
import com.jonahbauer.qed.util.StatusWrapper;

import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class EventListViewModel extends ViewModel implements QEDPageReceiver<List<Event>> {
    private final MutableLiveData<StatusWrapper<List<Event>>> mEvents = new MutableLiveData<>();
    private final CompositeDisposable mDisposable = new CompositeDisposable();

    public EventListViewModel() {
        mEvents.setValue(StatusWrapper.wrap(Collections.emptyList(), STATUS_LOADED));
    }

    public void load() {
        mEvents.setValue(StatusWrapper.wrap(Collections.emptyList(), STATUS_PRELOADED));
        mDisposable.add(
                QEDDBPages.getEventList(this)
        );
    }

    public LiveData<StatusWrapper<List<Event>>> getEvents() {
        return mEvents;
    }

    @Override
    public void onPageReceived(@NonNull List<Event> out) {
        if (out.size() > 0) {
            this.mEvents.setValue(StatusWrapper.wrap(out, STATUS_LOADED));
        } else {
            this.mEvents.setValue(StatusWrapper.wrap(Collections.emptyList(), Reason.EMPTY));
        }
    }

    @Override
    public void onError(List<Event> out, @NonNull Reason reason, @Nullable Throwable cause) {
        QEDPageReceiver.super.onError(out, reason, cause);
        this.mEvents.setValue(StatusWrapper.wrap(out, reason));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mDisposable.clear();
    }
}