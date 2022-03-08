package com.jonahbauer.qed.model.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.jonahbauer.qed.model.Person;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.networking.async.QEDPageReceiver;
import com.jonahbauer.qed.networking.pages.QEDDBPages;
import com.jonahbauer.qed.util.StatusWrapper;

import java.time.Instant;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class PersonViewModel extends ViewModel implements QEDPageReceiver<Person> {
    private final MutableLiveData<StatusWrapper<Person>> mPerson = new MutableLiveData<>();
    private final CompositeDisposable mDisposable = new CompositeDisposable();

    public void load(@NonNull Person person) {
        this.mPerson.setValue(StatusWrapper.preloaded(person));
        mDisposable.add(
                QEDDBPages.getPerson(person, this)
        );
    }

    public LiveData<StatusWrapper<Person>> getPerson() {
        return mPerson;
    }

    @Override
    public void onResult(@NonNull Person out) {
        out.setLoaded(Instant.now());
        this.mPerson.setValue(StatusWrapper.loaded(out));
    }

    @Override
    public void onError(Person out, @NonNull Reason reason, @Nullable Throwable cause) {
        QEDPageReceiver.super.onError(out, reason, cause);
        this.mPerson.setValue(StatusWrapper.error(out, reason));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mDisposable.clear();
    }
}