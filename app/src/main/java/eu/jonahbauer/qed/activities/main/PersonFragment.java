package eu.jonahbauer.qed.activities.main;

import androidx.annotation.NonNull;
import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.activities.sheets.InfoFragment;
import eu.jonahbauer.qed.activities.sheets.person.PersonInfoFragment;
import eu.jonahbauer.qed.model.Person;
import eu.jonahbauer.qed.model.viewmodel.InfoViewModel;
import eu.jonahbauer.qed.model.viewmodel.PersonViewModel;
import eu.jonahbauer.qed.util.ViewUtils;

public class PersonFragment extends MainInfoFragment {

    private PersonViewModel mPersonViewModel;

    @Override
    protected void onCreateViewModel() {
        PersonFragmentArgs args = PersonFragmentArgs.fromBundle(getArguments());
        Person person = args.getPerson();
        if (person == null) person = new Person(args.getId());

        mPersonViewModel = ViewUtils.getViewModelProvider(this, R.id.nav_person).get(PersonViewModel.class);
        mPersonViewModel.load(person);

        setDesignSeed(person.getId());
    }

    @Override
    protected @NonNull InfoFragment createFragment() {
        return PersonInfoFragment.newInstance();
    }

    @Override
    protected @NonNull InfoViewModel<?> getInfoViewModel() {
        return mPersonViewModel;
    }
}
