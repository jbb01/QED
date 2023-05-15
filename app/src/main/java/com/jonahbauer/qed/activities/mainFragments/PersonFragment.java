package com.jonahbauer.qed.activities.mainFragments;

import androidx.annotation.NonNull;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.sheets.InfoFragment;
import com.jonahbauer.qed.activities.sheets.person.PersonInfoFragment;
import com.jonahbauer.qed.model.Person;
import com.jonahbauer.qed.model.viewmodel.PersonViewModel;
import com.jonahbauer.qed.util.StatusWrapper;
import com.jonahbauer.qed.util.Themes;
import com.jonahbauer.qed.util.ViewUtils;

public class PersonFragment extends MainInfoFragment {

    private PersonViewModel mPersonViewModel;

    @Override
    public void onCreateViewModel() {
        PersonFragmentArgs args = PersonFragmentArgs.fromBundle(getArguments());
        Person person = args.getPerson();
        if (person == null) person = new Person(args.getId());

        mPersonViewModel = ViewUtils.getViewModelProvider(this, R.id.nav_person).get(PersonViewModel.class);
        mPersonViewModel.load(person);

        setColor(Themes.colorful(requireContext(), person.getId()));
    }

    @Override
    public void onObserveViewModel() {
        mPersonViewModel.getValueStatus().observe(getViewLifecycleOwner(), wrapper -> {
            Person value = wrapper.getValue();
            if (value != null) {
                ViewUtils.setActionBarText(this, value.getFullName());
            }

            if (wrapper.getCode() != StatusWrapper.STATUS_PRELOADED) {
                startPostponedEnterTransition();
            }
        });
    }

    @NonNull
    @Override
    public InfoFragment createFragment() {
        return PersonInfoFragment.newInstance();
    }
}
