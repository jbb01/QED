package com.jonahbauer.qed.activities.mainFragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.sheets.registration.RegistrationInfoFragment;
import com.jonahbauer.qed.databinding.SingleFragmentBinding;
import com.jonahbauer.qed.model.Registration;
import com.jonahbauer.qed.model.viewmodel.RegistrationViewModel;
import com.jonahbauer.qed.util.StatusWrapper;
import com.jonahbauer.qed.util.Themes;
import com.jonahbauer.qed.util.TransitionUtils;
import com.jonahbauer.qed.util.ViewUtils;

import java.util.concurrent.TimeUnit;

public class RegistrationFragment extends Fragment {

    private RegistrationViewModel mRegistrationViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RegistrationFragmentArgs args = RegistrationFragmentArgs.fromBundle(getArguments());
        Registration registration = args.getRegistration();
        if (registration == null) registration = new Registration(args.getId());

        mRegistrationViewModel = ViewUtils.getViewModelProvider(this, R.id.nav_registration).get(RegistrationViewModel.class);
        mRegistrationViewModel.load(registration);

        var color = Themes.colorful(requireContext(), registration.getId());
        TransitionUtils.setupDefaultTransitions(this, color);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        SingleFragmentBinding binding = SingleFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ViewUtils.setFitsSystemWindows(this);
        postponeEnterTransition(200, TimeUnit.MILLISECONDS);

        mRegistrationViewModel.getRegistration().observe(getViewLifecycleOwner(), wrapper -> {
            Registration value = wrapper.getValue();
            if (value != null) {
                ViewUtils.setActionBarText(this, getString(R.string.registration_title, value.getPersonName()));
            }

            if (wrapper.getCode() != StatusWrapper.STATUS_PRELOADED) {
                startPostponedEnterTransition();
            }
        });

        var manager = getChildFragmentManager();
        if (!(manager.findFragmentById(R.id.fragment) instanceof RegistrationInfoFragment)) {
            RegistrationInfoFragment fragment = RegistrationInfoFragment.newInstance();
            fragment.hideTitle();
            manager.beginTransaction().replace(R.id.fragment, fragment).commit();
        }
    }
}
