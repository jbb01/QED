package com.jonahbauer.qed.activities.sheets.person;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.jonahbauer.qed.activities.sheets.AbstractInfoBottomSheet;
import com.jonahbauer.qed.activities.sheets.AbstractInfoFragment;
import com.jonahbauer.qed.model.Person;
import com.jonahbauer.qed.model.viewmodel.PersonViewModel;
import com.jonahbauer.qed.util.StatusWrapper;
import com.jonahbauer.qed.util.Themes;

public class PersonInfoBottomSheet extends AbstractInfoBottomSheet {
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
            // TODO show toast
            dismiss();
            return;
        }

        mPersonViewModel = new ViewModelProvider(requireActivity()).get(PersonViewModel.class);
        mPersonViewModel.load(person);
    }

    @Override
    public int getColor() {
        return Themes.colorful(getPerson().getId());
    }

    @Override
    public AbstractInfoFragment createFragment() {
        return PersonInfoFragment.newInstance();
    }

    @NonNull
    private Person getPerson() {
        StatusWrapper<Person> wrapper = mPersonViewModel.getPerson().getValue();
        assert wrapper != null : "StatusWrapper should not be null";
        Person person = wrapper.getValue();
        assert person != null : "Person should not be null";
        return person;
    }
}
