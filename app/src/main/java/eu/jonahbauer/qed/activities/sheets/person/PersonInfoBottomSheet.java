package eu.jonahbauer.qed.activities.sheets.person;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.activities.sheets.InfoBottomSheet;
import eu.jonahbauer.qed.activities.sheets.InfoFragment;
import eu.jonahbauer.qed.model.Person;
import eu.jonahbauer.qed.model.viewmodel.InfoViewModel;
import eu.jonahbauer.qed.model.viewmodel.PersonViewModel;

import java.util.Objects;

public class PersonInfoBottomSheet extends InfoBottomSheet {
    private static final String ARGUMENT_PERSON = "person";

    private PersonViewModel mPersonViewModel;

    @NonNull
    public static PersonInfoBottomSheet newInstance(Person person) {
        Bundle args = new Bundle();
        args.putParcelable(ARGUMENT_PERSON, person);
        PersonInfoBottomSheet sheet = new PersonInfoBottomSheet();
        sheet.setArguments(args);
        return sheet;
    }

    public PersonInfoBottomSheet() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        assert args != null;

        Person person = args.getParcelable(ARGUMENT_PERSON);
        if (person == null) {
            Toast.makeText(requireContext(), R.string.error, Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        mPersonViewModel = getViewModelProvider().get(PersonViewModel.class);
        mPersonViewModel.load(person);
    }

    @Override
    public long getDesignSeed() {
        return getPerson().getId();
    }

    @Override
    public @NonNull InfoFragment createFragment() {
        return PersonInfoFragment.newInstance();
    }

    @Override
    public @NonNull InfoViewModel<?> getInfoViewModel() {
        return mPersonViewModel;
    }

    @NonNull
    private Person getPerson() {
        return Objects.requireNonNull(mPersonViewModel.getValue().getValue());
    }
}
