package com.jonahbauer.qed.activities.sheets.person;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.lifecycle.ViewModelProvider;

import com.jonahbauer.qed.activities.sheets.AbstractInfoBottomSheet;
import com.jonahbauer.qed.activities.sheets.AbstractInfoFragment;
import com.jonahbauer.qed.model.Person;
import com.jonahbauer.qed.util.Themes;

public class PersonInfoBottomSheet extends AbstractInfoBottomSheet {
    private static final String ARGUMENT_PERSON = "person";
    private static final String ARGUMENT_THEME_ID = "themeId";

    private PersonViewModel personViewModel;

    @NonNull
    public static PersonInfoBottomSheet newInstance(Person person, @StyleRes int themeId) {
        Bundle args = new Bundle();
        args.putParcelable(ARGUMENT_PERSON, person);
        args.putInt(ARGUMENT_THEME_ID, themeId);
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

        personViewModel = new ViewModelProvider(getActivity()).get(PersonViewModel.class);
        personViewModel.load(args.getParcelable(ARGUMENT_PERSON));
    }

    @Override
    public int getColor() {
        return Themes.colorful(getPerson().getId());
    }

    @Override
    public AbstractInfoFragment createFragment() {
        return PersonInfoFragment.newInstance();
    }

    private Person getPerson() {
        return personViewModel.getPerson().getValue().getValue();
    }
}
