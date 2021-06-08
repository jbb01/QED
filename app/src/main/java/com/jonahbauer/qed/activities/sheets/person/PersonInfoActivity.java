package com.jonahbauer.qed.activities.sheets.person;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.jonahbauer.qed.activities.sheets.AbstractInfoActivity;
import com.jonahbauer.qed.activities.sheets.AbstractInfoFragment;
import com.jonahbauer.qed.model.Person;
import com.jonahbauer.qed.model.viewmodel.PersonViewModel;
import com.jonahbauer.qed.util.StatusWrapper;
import com.jonahbauer.qed.util.Themes;

public class PersonInfoActivity extends AbstractInfoActivity {
    public static final String ARGUMENT_PERSON = "person";

    private PersonViewModel mPersonViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Bundle args = getIntent().getExtras();
        assert args != null;

        Person person = args.getParcelable(ARGUMENT_PERSON);
        if (person == null) {
            // TODO show toast
            finish();
            return;
        }

        mPersonViewModel = new ViewModelProvider(this).get(PersonViewModel.class);
        mPersonViewModel.load(person);

        super.onCreate(savedInstanceState);
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
