package com.jonahbauer.qed.activities.mainFragments;

import androidx.annotation.NonNull;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.sheets.InfoFragment;
import com.jonahbauer.qed.activities.sheets.registration.RegistrationInfoFragment;
import com.jonahbauer.qed.model.Registration;
import com.jonahbauer.qed.model.viewmodel.RegistrationViewModel;
import com.jonahbauer.qed.util.StatusWrapper;
import com.jonahbauer.qed.util.Themes;
import com.jonahbauer.qed.util.ViewUtils;

public class RegistrationFragment extends MainInfoFragment {

    private RegistrationViewModel mRegistrationViewModel;

    @Override
    public void onCreateViewModel() {
        RegistrationFragmentArgs args = RegistrationFragmentArgs.fromBundle(getArguments());
        Registration registration = args.getRegistration();
        if (registration == null) registration = new Registration(args.getId());

        mRegistrationViewModel = ViewUtils.getViewModelProvider(this, R.id.nav_registration).get(RegistrationViewModel.class);
        mRegistrationViewModel.load(registration);

        setColor(Themes.colorful(requireContext(), registration.getId()));
    }

    @Override
    public void onObserveViewModel() {
        mRegistrationViewModel.getValueStatus().observe(getViewLifecycleOwner(), wrapper -> {
            Registration value = wrapper.getValue();
            if (value != null) {
                ViewUtils.setActionBarText(this, getString(R.string.registration_title, value.getPersonName()));
            }

            if (wrapper.getCode() != StatusWrapper.STATUS_PRELOADED) {
                startPostponedEnterTransition();
            }
        });
    }

    @NonNull
    @Override
    public InfoFragment createFragment() {
        return RegistrationInfoFragment.newInstance();
    }
}
