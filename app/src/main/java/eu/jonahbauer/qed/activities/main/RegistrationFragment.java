package eu.jonahbauer.qed.activities.main;

import androidx.annotation.NonNull;
import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.activities.sheets.InfoFragment;
import eu.jonahbauer.qed.activities.sheets.registration.RegistrationInfoFragment;
import eu.jonahbauer.qed.model.Registration;
import eu.jonahbauer.qed.model.viewmodel.InfoViewModel;
import eu.jonahbauer.qed.model.viewmodel.RegistrationViewModel;
import eu.jonahbauer.qed.util.ViewUtils;

public class RegistrationFragment extends MainInfoFragment {

    private RegistrationViewModel mRegistrationViewModel;

    @Override
    protected void onCreateViewModel() {
        RegistrationFragmentArgs args = RegistrationFragmentArgs.fromBundle(getArguments());
        Registration registration = args.getRegistration();
        if (registration == null) registration = new Registration(args.getId());

        mRegistrationViewModel = ViewUtils.getViewModelProvider(this, R.id.nav_registration).get(RegistrationViewModel.class);
        mRegistrationViewModel.load(registration);

        setDesignSeed(registration.getId());
    }

    @Override
    protected @NonNull InfoFragment createFragment() {
        return RegistrationInfoFragment.newInstance();
    }

    @Override
    protected @NonNull InfoViewModel<?> getInfoViewModel() {
        return mRegistrationViewModel;
    }
}
