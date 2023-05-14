package com.jonahbauer.qed.activities.sheets.registration;

import android.app.PendingIntent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.navigation.NavDeepLinkBuilder;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.MainActivity;
import com.jonahbauer.qed.activities.mainFragments.EventFragmentArgs;
import com.jonahbauer.qed.activities.mainFragments.PersonFragmentArgs;
import com.jonahbauer.qed.activities.mainFragments.RegistrationFragmentArgs;
import com.jonahbauer.qed.activities.mainFragments.RegistrationFragmentDirections;
import com.jonahbauer.qed.activities.sheets.InfoFragment;
import com.jonahbauer.qed.databinding.FragmentInfoRegistrationBinding;
import com.jonahbauer.qed.model.Event;
import com.jonahbauer.qed.model.Person;
import com.jonahbauer.qed.model.Registration;
import com.jonahbauer.qed.model.viewmodel.RegistrationViewModel;
import com.jonahbauer.qed.networking.NetworkConstants;
import com.jonahbauer.qed.util.StatusWrapper;
import com.jonahbauer.qed.util.Themes;

import java.util.Locale;

public class RegistrationInfoFragment extends InfoFragment {
    private static final String SAVED_TITLE_HIDDEN = "titleHidden";

    private RegistrationViewModel mRegistrationViewModel;
    private FragmentInfoRegistrationBinding mBinding;

    private boolean mHideTitle;

    public static RegistrationInfoFragment newInstance() {
        return new RegistrationInfoFragment();
    }

    public RegistrationInfoFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRegistrationViewModel = getViewModelProvider(R.id.nav_registration).get(RegistrationViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentInfoRegistrationBinding.inflate(inflater, container, false);
        mRegistrationViewModel.getRegistration().observe(getViewLifecycleOwner(), registrationStatusWrapper -> {
            var value = registrationStatusWrapper.getValue();
            var code = registrationStatusWrapper.getCode();
            mBinding.setRegistration(value);
            mBinding.setLoading(code == StatusWrapper.STATUS_PRELOADED);
            mBinding.setError(code == StatusWrapper.STATUS_ERROR ? getString(R.string.error_incomplete) : null);
        });
        mBinding.setColor(getColor());
        if (mHideTitle) hideTitle();
        return mBinding.getRoot();
    }

    @NonNull
    private Registration getRegistration() {
        var wrapper = mRegistrationViewModel.getRegistration().getValue();
        assert wrapper != null : "StatusWrapper should not be null";
        var registration = wrapper.getValue();
        assert registration != null : "Registration should not be null";
        return registration;
    }

    @Override
    public int getColor() {
        return Themes.colorful(requireContext(), getRegistration().getId());
    }

    @Override
    protected int getBackground() {
        return Themes.pattern(getRegistration().getId());
    }

    @Override
    protected String getTitle() {
        return getString(R.string.registration_title, getRegistration().getPersonName());
    }

    @Override
    protected float getTitleBottom() {
        return mBinding.title.getBottom();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(SAVED_TITLE_HIDDEN)) hideTitle();
        }

        mBinding.listItemPerson.setOnClickListener(v -> {
            var registration = mBinding.getRegistration();
            if (registration != null) {
                var person = registration.getPerson();
                if (person == null) {
                    Snackbar.make(v, R.string.registration_snackbar_person_not_available, Snackbar.LENGTH_SHORT).show();
                } else {
                    showPerson(v, registration, person);
                }
            }
        });
        mBinding.listItemEvent.setOnClickListener(v -> {
            var registration = mBinding.getRegistration();
            if (registration != null) {
                var event = registration.getEvent();
                if (event == null) {
                    Snackbar.make(v, R.string.registration_snackbar_event_not_available, Snackbar.LENGTH_SHORT).show();
                } else {
                    showEvent(v, registration, event);
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_TITLE_HIDDEN, mHideTitle);
    }

    @Override
    public void hideTitle() {
        mHideTitle = true;
        if (mBinding != null) {
            mBinding.titleLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean isOpenInBrowserSupported() {
        return true;
    }

    @Override
    public @NonNull String getOpenInBrowserLink() {
        return String.format(Locale.ROOT, NetworkConstants.DATABASE_SERVER_REGISTRATION, getRegistration().getId());
    }

    private static void showEvent(View view, Registration registration, Event event) {
        try {
            var navController = Navigation.findNavController(view);

            var action = RegistrationFragmentDirections.showEvent(event.getId());
            action.setEvent(event);
            navController.navigate(action);
        } catch (IllegalStateException e) {
            try {
                var intent = new NavDeepLinkBuilder(view.getContext())
                        .setComponentName(MainActivity.class)
                        .setGraph(R.navigation.main)
                        .addDestination(R.id.nav_registration, new RegistrationFragmentArgs.Builder(registration.getId()).setRegistration(registration).build().toBundle())
                        .addDestination(R.id.nav_event, new EventFragmentArgs.Builder(event.getId()).setEvent(event).build().toBundle())
                        .createPendingIntent();
                intent.send();
            } catch (PendingIntent.CanceledException ignored) {}
        }
    }

    private static void showPerson(View view, Registration registration, Person person) {
        try {
            var navController = Navigation.findNavController(view);

            var action = RegistrationFragmentDirections.showPerson(person.getId());
            action.setPerson(person);
            navController.navigate(action);
        } catch (IllegalStateException e) {
            try {
                var intent = new NavDeepLinkBuilder(view.getContext())
                        .setComponentName(MainActivity.class)
                        .setGraph(R.navigation.main)
                        .addDestination(R.id.nav_registration, new RegistrationFragmentArgs.Builder(registration.getId()).setRegistration(registration).build().toBundle())
                        .addDestination(R.id.nav_person, new PersonFragmentArgs.Builder(person.getId()).setPerson(person).build().toBundle())
                        .createPendingIntent();
                intent.send();
            } catch (PendingIntent.CanceledException ignored) {}
        }
    }
}
