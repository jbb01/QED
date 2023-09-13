package eu.jonahbauer.qed.activities.mainFragments;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;

import java.util.function.Supplier;

import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.activities.sheets.InfoFragment;
import eu.jonahbauer.qed.activities.sheets.person.PersonInfoFragment;
import eu.jonahbauer.qed.model.Person;
import eu.jonahbauer.qed.model.viewmodel.InfoViewModel;
import eu.jonahbauer.qed.model.viewmodel.PersonViewModel;
import eu.jonahbauer.qed.util.Actions;
import eu.jonahbauer.qed.util.Themes;
import eu.jonahbauer.qed.util.ViewUtils;
import lombok.RequiredArgsConstructor;

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
    public final void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        PersonFragmentArgs args = PersonFragmentArgs.fromBundle(getArguments());
        Person person = args.getPerson();
        if (person != null) {
            var menuProvider = new ExportContactMenuProvider(person);
            requireActivity().addMenuProvider(menuProvider, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        }
    }



    @Override
    public @NonNull InfoFragment createFragment() {
        return PersonInfoFragment.newInstance();
    }

    @Override
    public @NonNull InfoViewModel<?> getInfoViewModel() {
        return mPersonViewModel;
    }

    @RequiredArgsConstructor
    private class ExportContactMenuProvider implements MenuProvider {

        @NonNull
        private final Person person;

        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            menuInflater.inflate(R.menu.menu_export_contact, menu);
        }

        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
            if (menuItem.getItemId() == R.id.export_contact) {
                Actions.exportToAddressBook(getContext(), person);
                return true;
            }
            return false;
        }
    }
}
