package com.jonahbauer.qed.model.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.jonahbauer.qed.model.Registration;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.networking.async.QEDPageReceiver;
import com.jonahbauer.qed.networking.pages.QEDDBPages;
import com.jonahbauer.qed.util.StatusWrapper;

import java.time.Instant;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class RegistrationViewModel extends ViewModel implements QEDPageReceiver<Registration> {
    private final MutableLiveData<StatusWrapper<Registration>> mRegistration = new MutableLiveData<>();
    private final CompositeDisposable mDisposable = new CompositeDisposable();

    public void load(@NonNull Registration registration) {
        this.mRegistration.setValue(StatusWrapper.preloaded(registration));
        mDisposable.add(
                QEDDBPages.getRegistration(registration, this)
        );
    }

    public LiveData<StatusWrapper<Registration>> getRegistration() {
        return mRegistration;
    }

    @Override
    public void onResult(@NonNull Registration out) {
        out.setLoaded(Instant.now());
        this.mRegistration.setValue(StatusWrapper.loaded(out));
    }

    @Override
    public void onError(Registration out, @NonNull Reason reason, @Nullable Throwable cause) {
        QEDPageReceiver.super.onError(out, reason, cause);
        this.mRegistration.setValue(StatusWrapper.error(out, reason));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mDisposable.clear();
    }
}