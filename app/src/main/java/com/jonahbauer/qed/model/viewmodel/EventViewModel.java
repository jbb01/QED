package com.jonahbauer.qed.model.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.jonahbauer.qed.model.Event;
import com.jonahbauer.qed.networking.QEDDBPages;
import com.jonahbauer.qed.networking.async.QEDPageReceiver;
import com.jonahbauer.qed.util.StatusWrapper;

import static com.jonahbauer.qed.util.StatusWrapper.STATUS_ERROR;
import static com.jonahbauer.qed.util.StatusWrapper.STATUS_LOADED;
import static com.jonahbauer.qed.util.StatusWrapper.STATUS_PRELOADED;

public class EventViewModel extends ViewModel implements QEDPageReceiver<Event> {
    private final MutableLiveData<StatusWrapper<Event>> event = new MutableLiveData<>();

    public void load(@NonNull Event event) {
        if (!event.isLoaded()) {
            this.event.setValue(StatusWrapper.wrap(event, STATUS_PRELOADED));
            QEDDBPages.getEvent(event, this);
        } else {
            onPageReceived(event);
        }
    }

    public LiveData<StatusWrapper<Event>> getEvent() {
        return event;
    }


    @Override
    public void onPageReceived(@NonNull Event out) {
        out.setLoaded(true);
        this.event.setValue(StatusWrapper.wrap(out, STATUS_LOADED));
    }

    @Override
    public void onError(Event out, String reason, @Nullable Throwable cause) {
        QEDPageReceiver.super.onError(out, reason, cause);
        this.event.setValue(StatusWrapper.wrap(out, STATUS_ERROR, reason));
    }
}