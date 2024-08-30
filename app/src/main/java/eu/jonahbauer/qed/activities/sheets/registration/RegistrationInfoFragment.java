package eu.jonahbauer.qed.activities.sheets.registration;

import android.app.PendingIntent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavDeepLinkBuilder;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;
import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.activities.MainActivity;
import eu.jonahbauer.qed.activities.sheets.InfoFragment;
import eu.jonahbauer.qed.databinding.FragmentInfoRegistrationBinding;
import eu.jonahbauer.qed.model.Event;
import eu.jonahbauer.qed.model.Person;
import eu.jonahbauer.qed.model.Registration;
import eu.jonahbauer.qed.model.viewmodel.RegistrationViewModel;
import eu.jonahbauer.qed.networking.NetworkConstants;
import eu.jonahbauer.qed.activities.mainFragments.EventFragmentArgs;
import eu.jonahbauer.qed.activities.mainFragments.PersonFragmentArgs;
import eu.jonahbauer.qed.activities.mainFragments.RegistrationFragmentArgs;
import eu.jonahbauer.qed.activities.mainFragments.RegistrationFragmentDirections;

import java.util.Locale;
import java.util.Objects;

public class RegistrationInfoFragment extends InfoFragment {
    private RegistrationViewModel mRegistrationViewModel;
    private FragmentInfoRegistrationBinding mBinding;

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
        mRegistrationViewModel.getValue().observe(getViewLifecycleOwner(), mBinding::setRegistration);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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

    private @NonNull Registration getRegistration() {
        return Objects.requireNonNull(mRegistrationViewModel.getValue().getValue());
    }

    @Override
    protected long getDesignSeed() {
        return getRegistration().getId();
    }

    @Override
    protected boolean isOpenInBrowserSupported() {
        return true;
    }

    @Override
    protected @NonNull String getOpenInBrowserLink() {
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
