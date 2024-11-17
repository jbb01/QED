package eu.jonahbauer.qed.activities.mainFragments;

import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.databinding.FragmentPersonsDatabaseBinding;
import eu.jonahbauer.qed.model.Person;
import eu.jonahbauer.qed.model.PersonFilter;
import eu.jonahbauer.qed.ui.adapter.PersonAdapter;
import eu.jonahbauer.qed.model.viewmodel.PersonListViewModel;
import eu.jonahbauer.qed.networking.Reason;
import eu.jonahbauer.qed.util.StatusWrapper;
import eu.jonahbauer.qed.util.TransitionUtils;
import eu.jonahbauer.qed.util.ViewUtils;

import java.util.ArrayList;

public class PersonDatabaseFragment extends Fragment implements AdapterView.OnItemClickListener, MenuProvider {
    private PersonAdapter mPersonAdapter;
    private FragmentPersonsDatabaseBinding mBinding;
    private MenuItem mRefresh;

    private PersonListViewModel mPersonListViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TransitionUtils.setupDefaultTransitions(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentPersonsDatabaseBinding.inflate(inflater, container, false);
        mPersonListViewModel = ViewUtils.getViewModelProvider(this, R.id.nav_database_persons).get(PersonListViewModel.class);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TransitionUtils.postponeEnterAnimationToPreDraw(this, view);
        requireActivity().addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        mPersonAdapter = new PersonAdapter(requireContext(), new ArrayList<>(), PersonAdapter.SortMode.FIRST_NAME, mBinding.fixedHeader.getRoot());
        mBinding.list.setOnItemClickListener(this);
        mBinding.list.setOnScrollListener(mPersonAdapter);
        mBinding.list.setAdapter(mPersonAdapter);

        mPersonListViewModel.getPersons().observe(getViewLifecycleOwner(), persons -> {
            mBinding.setStatus(persons.getCode());

            if (mRefresh != null) {
                mRefresh.setEnabled(persons.getCode() != StatusWrapper.STATUS_PRELOADED);
            }

            mPersonAdapter.clear();
            int hits = 0;
            if (persons.getCode() == StatusWrapper.STATUS_LOADED) {
                mPersonAdapter.setSortMode(mPersonListViewModel.getSortMode());
                mPersonAdapter.setPersons(persons.getValue());
                hits = persons.getValue().size();
            } else if (persons.getCode() == StatusWrapper.STATUS_ERROR) {
                Reason reason = persons.getReason();
                mBinding.setError(getString(reason == Reason.EMPTY ? R.string.database_empty : reason.getStringRes()));
            }
            mPersonAdapter.notifyDataSetChanged();

            if (hits > 0) {
                mBinding.setHits(getString(R.string.hits, hits));
            } else {
                mBinding.setHits("");
            }
        });

        mBinding.searchButton.setOnClickListener(v -> search());

        ViewUtils.setupExpandable(mBinding.expandCheckBox, mBinding.expandable, mPersonListViewModel.getExpanded());
        ViewUtils.link(mBinding.databaseFirstNameCheckbox, mBinding.databaseFirstNameRow, mBinding.databaseFirstNameEditText);
        ViewUtils.link(mBinding.databaseLastNameCheckbox, mBinding.databaseLastNameRow, mBinding.databaseLastNameEditText);
    }

    private PersonAdapter.SortMode getSortMode() {
        if (mBinding.databaseSortFirstNameRadioButton.isChecked()) {
            return PersonAdapter.SortMode.FIRST_NAME;
        } else {
            return PersonAdapter.SortMode.LAST_NAME;
        }
    }

    private void search() {
        String firstName = null;
        String lastName = null;
        Boolean member;
        Boolean active;
        Boolean favorite;

        if (mBinding.databaseFirstNameCheckbox.isChecked()) {
            firstName = mBinding.databaseFirstNameEditText.getText().toString();
        }

        if (mBinding.databaseLastNameCheckbox.isChecked()) {
            lastName = mBinding.databaseLastNameEditText.getText().toString();
        }

        member = mBinding.databaseMemberCheckbox.getState().asBoolean();
        active = mBinding.databaseActiveCheckbox.getState().asBoolean();
        favorite = mBinding.databaseFavoriteCheckbox.getState().asBoolean();

        mPersonListViewModel.setSortMode(getSortMode());
        mPersonListViewModel.filter(new PersonFilter(firstName, lastName, member, active, favorite));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Person person = mPersonAdapter.getPerson(position);
        if (person != null) {
            var extras = new FragmentNavigator.Extras.Builder()
                    .addSharedElement(view, getString(R.string.transition_name_single_fragment))
                    .build();
            var action = PersonDatabaseFragmentDirections.showPerson(person.getId());
            action.setPerson(person);
            Navigation.findNavController(view).navigate(action, extras);

            TransitionUtils.setupReenterElevationScale(this);
        }
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_person_database, menu);
        mRefresh = menu.findItem(R.id.menu_refresh);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            mPersonListViewModel.load();

            Drawable icon = mRefresh.getIcon();
            if (icon instanceof Animatable) ((Animatable) icon).start();

            return true;
        }
        return false;
    }
}