package com.jonahbauer.qed.model.viewmodel;

import static com.jonahbauer.qed.util.StatusWrapper.STATUS_ERROR;
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

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class EventViewModel extends ViewModel implements QEDPageReceiver<Event> {
    private final MutableLiveData<StatusWrapper<Event>> mEvent = new MutableLiveData<>();
    private final CompositeDisposable mDisposable = new CompositeDisposable();

    public void load(@NonNull Event event) {
        if (!event.isLoaded()) {
            this.mEvent.setValue(StatusWrapper.wrap(event, STATUS_PRELOADED));
            mDisposable.add(
                    QEDDBPages.getEvent(event, this)
            );
        } else {
            onPageReceived(event);
        }
    }

    public LiveData<StatusWrapper<Event>> getEvent() {
        return mEvent;
    }


    @Override
    public void onPageReceived(@NonNull Event out) {
        out.setLoaded(true);
        this.mEvent.setValue(StatusWrapper.wrap(out, STATUS_LOADED));
    }

    @Override
    public void onError(Event out, @NonNull Reason reason, @Nullable Throwable cause) {
        QEDPageReceiver.super.onError(out, reason, cause);
        this.mEvent.setValue(StatusWrapper.wrap(out, STATUS_ERROR, reason));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mDisposable.clear();
    }
}