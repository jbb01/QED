package eu.jonahbauer.qed.activities.sheets.registration;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.activities.sheets.InfoBottomSheet;
import eu.jonahbauer.qed.activities.sheets.InfoFragment;
import eu.jonahbauer.qed.model.Registration;
import eu.jonahbauer.qed.model.viewmodel.InfoViewModel;
import eu.jonahbauer.qed.model.viewmodel.RegistrationViewModel;

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
    public long getDesignSeed() {
        return getRegistration().getId();
    }

    @Override
    public @NonNull InfoFragment createFragment() {
        return RegistrationInfoFragment.newInstance();
    }

    @Override
    public @NonNull InfoViewModel<?> getInfoViewModel() {
        return mRegistrationViewModel;
    }

    @NonNull
    private Registration getRegistration() {
        return Objects.requireNonNull(mRegistrationViewModel.getValue().getValue());
    }
}
