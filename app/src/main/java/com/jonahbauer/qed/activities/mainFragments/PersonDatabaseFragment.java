package com.jonahbauer.qed.activities.mainFragments;

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
import android.widget.CompoundButton;
import android.widget.RadioButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.databinding.FragmentPersonsDatabaseBinding;
import com.jonahbauer.qed.layoutStuff.views.CheckBoxTriStates;
import com.jonahbauer.qed.model.Person;
import com.jonahbauer.qed.model.adapter.PersonAdapter;
import com.jonahbauer.qed.model.viewmodel.PersonListViewModel;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.util.StatusWrapper;
import com.jonahbauer.qed.util.TransitionUtils;
import com.jonahbauer.qed.util.ViewUtils;

import java.util.ArrayList;
import java.util.Objects;

public class PersonDatabaseFragment extends Fragment implements CompoundButton.OnCheckedChangeListener, AdapterView.OnItemClickListener {
    private PersonAdapter mPersonAdapter;
    private FragmentPersonsDatabaseBinding mBinding;
    private MenuItem mRefresh;

    private PersonListViewModel mPersonListViewModel;

    private boolean mSortLastName = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

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
        ViewUtils.setFitsSystemWindows(this);
        TransitionUtils.postponeEnterAnimationToPreDraw(this, view);

        mPersonAdapter = new PersonAdapter(getContext(), new ArrayList<>(), PersonAdapter.SortMode.FIRST_NAME, mBinding.fixedHeader.getRoot());
        mBinding.list.setOnItemClickListener(this);
        mBinding.list.setOnScrollListener(mPersonAdapter);
        mBinding.list.setAdapter(mPersonAdapter);

        mPersonListViewModel.getPersons().observe(getViewLifecycleOwner(), persons -> {
            mBinding.setStatus(persons.getCode());

            if (mRefresh != null) {
                mRefresh.setEnabled(persons.getCode() != StatusWrapper.STATUS_PRELOADED);
            }

            mPersonAdapter.clear();
            if (persons.getCode() == StatusWrapper.STATUS_LOADED) {
                mPersonAdapter.addAll(persons.getValue());
            } else if (persons.getCode() == StatusWrapper.STATUS_ERROR) {
                Reason reason = persons.getReason();
                mBinding.setError(getString(reason == Reason.EMPTY ? R.string.database_empty : reason.getStringRes()));
            }
            mPersonAdapter.notifyDataSetChanged();

            int hits = mPersonAdapter.getItemList().size();
            if (hits > 0) {
                mBinding.setHits(getString(R.string.hits, hits));
            } else {
                mBinding.setHits("");
            }
        });

        mBinding.searchButton.setOnClickListener(v -> search());
        mBinding.databaseSortFirstNameRadioButton.setOnClickListener(this::onRadioButtonClicked);
        mBinding.databaseSortLastNameRadioButton.setOnClickListener(this::onRadioButtonClicked);

        mBinding.expandCheckBox.setOnCheckedChangeListener(this);

        mBinding.databaseFirstNameCheckbox.setOnCheckedChangeListener(this);
        mBinding.databaseLastNameCheckbox.setOnCheckedChangeListener(this);
        mBinding.databaseActiveCheckbox.setOnCheckedChangeListener(this);
        mBinding.databaseMemberCheckbox.setOnCheckedChangeListener(this);

        onCheckedChanged(mBinding.databaseFirstNameCheckbox, mBinding.databaseFirstNameCheckbox.isChecked());
        onCheckedChanged(mBinding.databaseLastNameCheckbox, mBinding.databaseLastNameCheckbox.isChecked());
    }

    @Override
    public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
        int id = buttonView.getId();
        if (id == R.id.expand_checkBox) {
            if (isChecked) {
                buttonView.setButtonDrawable(R.drawable.ic_arrow_up_accent_animation);
                ((Animatable) Objects.requireNonNull(buttonView.getButtonDrawable())).start();
                ViewUtils.expand(mBinding.expandable);
            } else {
                buttonView.setButtonDrawable(R.drawable.ic_arrow_down_accent_animation);
                ((Animatable) Objects.requireNonNull(buttonView.getButtonDrawable())).start();
                ViewUtils.collapse(mBinding.expandable);
            }
        } else if (id == R.id.database_firstName_checkbox) {
            mBinding.databaseFirstNameEditText.setEnabled(isChecked);
            if (isChecked) mBinding.databaseFirstNameEditText.requestFocus();
        } else if (id == R.id.database_lastName_checkbox) {
            mBinding.databaseLastNameEditText.setEnabled(isChecked);
            if (isChecked) mBinding.databaseLastNameEditText.requestFocus();
        }
    }

    private void onRadioButtonClicked(@NonNull View view) {
        boolean checked = ((RadioButton) view).isChecked();

        int id = view.getId();
        if (id == R.id.database_sort_first_name_radio_button) {
            if (checked) mSortLastName = false;
        } else if (id == R.id.database_sort_last_name_radio_button) {
            if (checked) mSortLastName = true;
        }
    }

    private void search() {
        String firstName = null;
        String lastName = null;
        Boolean member = null;
        Boolean active = null;

        if (mBinding.databaseFirstNameCheckbox.isChecked()) {
            firstName = mBinding.databaseFirstNameEditText.getText().toString().trim();
        }

        if (mBinding.databaseLastNameCheckbox.isChecked()) {
            lastName = mBinding.databaseLastNameEditText.getText().toString().trim();
        }

        int memberCheckbox = mBinding.databaseMemberCheckbox.getState();
        if (memberCheckbox == CheckBoxTriStates.CHECKED) {
            member = true;
        } else if (memberCheckbox == CheckBoxTriStates.UNCHECKED) {
            member = false;
        }

        int activeCheckbox = mBinding.databaseActiveCheckbox.getState();
        if (activeCheckbox == CheckBoxTriStates.CHECKED) {
            active = true;
        } else if (activeCheckbox == CheckBoxTriStates.UNCHECKED) {
            active = false;
        }

        mPersonAdapter.setSortMode(mSortLastName ? PersonAdapter.SortMode.LAST_NAME : PersonAdapter.SortMode.FIRST_NAME);
        mPersonListViewModel.filter(firstName, lastName, member, active);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Person person = mPersonAdapter.getItem(position);
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
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_person_database, menu);
        mRefresh = menu.findItem(R.id.menu_refresh);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            mPersonListViewModel.load();

            Drawable icon = mRefresh.getIcon();
            if (icon instanceof Animatable) ((Animatable) icon).start();

            return true;
        }
        return false;
    }
}