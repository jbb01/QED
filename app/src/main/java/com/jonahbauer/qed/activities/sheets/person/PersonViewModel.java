package com.jonahbauer.qed.activities.sheets.person;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.jonahbauer.qed.model.Person;
import com.jonahbauer.qed.networking.QEDDBPages;
import com.jonahbauer.qed.networking.async.QEDPageReceiver;
import com.jonahbauer.qed.util.StatusWrapper;

public class PersonViewModel extends ViewModel implements QEDPageReceiver<Person> {
    public static final int STATUS_PRELOADED = 0;
    public static final int STATUS_LOADED = 1;
    public static final int STATUS_ERROR = -1;

    private final MutableLiveData<StatusWrapper<Person>> person = new MutableLiveData<>();

    public void load(Person person) {
        if (!person.isLoaded()) {
            this.person.setValue(StatusWrapper.wrap(person, STATUS_PRELOADED));
            QEDDBPages.getPerson(person, this);
        } else {
            onPageReceived(person);
        }
    }

    public LiveData<StatusWrapper<Person>> getPerson() {
        return person;
    }

    @Override
    public void onPageReceived(@Nullable Person out) {
        out.setLoaded(true);
        this.person.setValue(StatusWrapper.wrap(out, STATUS_LOADED));
    }

    @Override
    public void onError(Person out, String reason, @Nullable Throwable cause) {
        QEDPageReceiver.super.onError(out, reason, cause);
        this.person.setValue(StatusWrapper.wrap(out, STATUS_ERROR, reason));
    }
}