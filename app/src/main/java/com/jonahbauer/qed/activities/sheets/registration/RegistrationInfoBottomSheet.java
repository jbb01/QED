package com.jonahbauer.qed.activities.sheets.registration;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.sheets.InfoBottomSheet;
import com.jonahbauer.qed.activities.sheets.InfoFragment;
import com.jonahbauer.qed.model.Registration;
import com.jonahbauer.qed.model.viewmodel.RegistrationViewModel;
import com.jonahbauer.qed.util.Themes;

import java.util.Objects;

public class RegistrationInfoBottomSheet extends InfoBottomSheet {
    private static final String ARGUMENT_REGISTRATION = "registration";

    private RegistrationViewModel mRegistrationViewModel;

    @NonNull
    public static RegistrationInfoBottomSheet newInstance(Registration registration) {
        Bundle args = new Bundle();
        args.putParcelable(ARGUMENT_REGISTRATION, registration);
        RegistrationInfoBottomSheet sheet = new RegistrationInfoBottomSheet();
        sheet.setArguments(args);
        return sheet;
    }

    public RegistrationInfoBottomSheet() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        assert args != null;

        Registration registration = args.getParcelable(ARGUMENT_REGISTRATION);
        if (registration == null) {
            Toast.makeText(requireContext(), R.string.error, Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        mRegistrationViewModel = getViewModelProvider().get(RegistrationViewModel.class);
        mRegistrationViewModel.load(registration);
    }

    @Override
    public int getColor() {
        return Themes.colorful(requireContext(), getRegistration().getId());
    }

    @Override
    public int getBackground() {
        return Themes.pattern(getRegistration().getId());
    }

    @NonNull
    @Override
    public InfoFragment createFragment() {
        return RegistrationInfoFragment.newInstance();
    }

    @NonNull
    private Registration getRegistration() {
        return Objects.requireNonNull(mRegistrationViewModel.getValue().getValue());
    }
}
