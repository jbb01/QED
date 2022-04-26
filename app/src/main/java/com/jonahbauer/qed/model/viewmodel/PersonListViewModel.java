package com.jonahbauer.qed.model.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.jonahbauer.qed.model.Person;
import com.jonahbauer.qed.model.PersonFilter;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.networking.async.QEDPageReceiver;
import com.jonahbauer.qed.networking.pages.QEDDBPages;
import com.jonahbauer.qed.util.StatusWrapper;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class PersonListViewModel extends ViewModel implements QEDPageReceiver<List<Person>> {
    private final MutableLiveData<StatusWrapper<List<Person>>> mPersons = new MutableLiveData<>();
    private final MutableLiveData<PersonFilter> mFilter = new MutableLiveData<>(PersonFilter.EMPTY);

    private final MediatorLiveData<StatusWrapper<List<Person>>> mFilteredPersons = new MediatorLiveData<>();

    private final CompositeDisposable mDisposable = new CompositeDisposable();

    public PersonListViewModel() {
        BiConsumer<StatusWrapper<List<Person>>, Predicate<Person>> observer = (wrapper, filter) -> {
            if (wrapper == null || wrapper.getValue() == null || wrapper.getValue().isEmpty()) {
                mFilteredPersons.setValue(wrapper);
            } else {
                mFilteredPersons.setValue(StatusWrapper.wrap(
                        wrapper.getValue().stream().filter(filter).collect(Collectors.toList()),
                        wrapper.getCode(),
                        wrapper.getReason()
                ));
            }
        };

        mFilteredPersons.addSource(mFilter, filter -> observer.accept(mPersons.getValue(), filter));
        mFilteredPersons.addSource(mPersons, wrapper -> observer.accept(wrapper, mFilter.getValue()));

        load();
    }

    public void load() {
        mPersons.setValue(StatusWrapper.preloaded(Collections.emptyList()));
        mDisposable.add(
                QEDDBPages.getPersonList(this)
        );
    }

    public void filter(@NonNull PersonFilter filter) {
        mFilter.setValue(filter);
    }

    public LiveData<StatusWrapper<List<Person>>> getPersons() {
        return mFilteredPersons;
    }

    @Override
    public void onResult(@NonNull List<Person> out) {
        if (out.size() > 0) {
            this.mPersons.setValue(StatusWrapper.loaded(out));
        } else {
            this.mPersons.setValue(StatusWrapper.error(Collections.emptyList(), Reason.EMPTY));
        }
    }

    @Override
    public void onError(List<Person> out, @NonNull Reason reason, @Nullable Throwable cause) {
        QEDPageReceiver.super.onError(out, reason, cause);
        this.mPersons.setValue(StatusWrapper.error(out, reason));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mDisposable.clear();
    }
}