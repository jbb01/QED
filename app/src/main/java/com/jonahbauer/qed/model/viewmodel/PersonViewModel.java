package com.jonahbauer.qed.model.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.jonahbauer.qed.model.Person;
import com.jonahbauer.qed.networking.QEDDBPages;
import com.jonahbauer.qed.networking.async.QEDPageReceiver;
import com.jonahbauer.qed.util.StatusWrapper;

import static com.jonahbauer.qed.util.StatusWrapper.STATUS_ERROR;
import static com.jonahbauer.qed.util.StatusWrapper.STATUS_LOADED;
import static com.jonahbauer.qed.util.StatusWrapper.STATUS_PRELOADED;

public class PersonViewModel extends ViewModel implements QEDPageReceiver<Person> {
    private final MutableLiveData<StatusWrapper<Person>> person = new MutableLiveData<>();

    public void load(@NonNull Person person) {
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
    public void onPageReceived(@NonNull Person out) {
        out.setLoaded(true);
        this.person.setValue(StatusWrapper.wrap(out, STATUS_LOADED));
    }

    @Override
    public void onError(Person out, String reason, @Nullable Throwable cause) {
        QEDPageReceiver.super.onError(out, reason, cause);
        this.person.setValue(StatusWrapper.wrap(out, STATUS_ERROR, reason));
    }
}