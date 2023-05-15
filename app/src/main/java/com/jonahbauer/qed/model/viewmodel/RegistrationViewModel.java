package com.jonahbauer.qed.model.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;

import androidx.annotation.StringRes;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.model.Registration;
import com.jonahbauer.qed.networking.async.QEDPageReceiver;
import com.jonahbauer.qed.networking.pages.QEDDBPages;

import java.time.Instant;

import io.reactivex.rxjava3.disposables.Disposable;

public class RegistrationViewModel extends DatabaseInfoViewModel<Registration> {

    public RegistrationViewModel(@NonNull Application application) {
        super(application);
    }

    @Override
    protected Disposable load(@NonNull Registration registration, @NonNull QEDPageReceiver<Registration> receiver) {
        return QEDDBPages.getRegistration(registration, receiver);
    }

    @Override
    public void onResult(@NonNull Registration out) {
        out.setLoaded(Instant.now());
    }

    @NonNull
    @Override
    protected CharSequence getTitle(@NonNull Registration registration) {
        return getApplication().getString(R.string.registration_title, registration.getPersonName());
    }

    @Override
    protected @StringRes int getDefaultTitle() {
        return R.string.title_fragment_registration;
    }
}