package com.jonahbauer.qed.activities.mainFragments;

import android.content.res.Configuration;
import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.fragment.app.FragmentManager;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.personSheet.PersonBottomSheet;
import com.jonahbauer.qed.activities.personSheet.PersonSideSheet;
import com.jonahbauer.qed.layoutStuff.CheckBoxTriStates;
import com.jonahbauer.qed.networking.QEDDBPages;
import com.jonahbauer.qed.networking.QEDPageReceiver;
import com.jonahbauer.qed.qeddb.person.Person;
import com.jonahbauer.qed.qeddb.person.PersonAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PersonDatabaseFragment extends QEDFragment implements CompoundButton.OnCheckedChangeListener, QEDPageReceiver<List<Person>> {
    private PersonAdapter mPersonAdapter;

    private ListView mPersonListView;
    private ArrayList<Person> mPersons;
    private View mExpand;
    private ProgressBar mSearchProgress;
    private CheckBox mFirstNameCheckBox;
    private CheckBox mLastNameCheckBox;
    private CheckBoxTriStates mMemberCheckBox;
    private CheckBoxTriStates mActiveCheckBox;
    private EditText mFirstNameEditText;
    private EditText mLastNameEditText;
    private TextView mHitsView;
    private TextView mErrorLabel;
    private Button mSearchButton;

    private boolean mSortLastName = false;

    public static PersonDatabaseFragment newInstance(@StyleRes int themeId) {
        Bundle args = new Bundle();

        args.putInt(ARGUMENT_THEME_ID, themeId);
        args.putInt(ARGUMENT_LAYOUT_ID, R.layout.fragment_persons_database);

        PersonDatabaseFragment fragment = new PersonDatabaseFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPersons = new ArrayList<>();
    }

    @Override
    public void onStart() {
        super.onStart();

        mSearchProgress.setVisibility(View.VISIBLE);
        mErrorLabel.setVisibility(View.GONE);
        mPersonListView.setVisibility(View.GONE);
        mSearchButton.setEnabled(false);
        QEDDBPages.getPersonList(getClass().toString(), this);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        CheckBox expandCheckBox = view.findViewById(R.id.expand_checkBox);
        mPersonListView = view.findViewById(R.id.person_list_view);
        mExpand = view.findViewById(R.id.expandable);
        mSearchButton = view.findViewById(R.id.search_button);
        mSearchProgress = view.findViewById(R.id.search_progress);
        mFirstNameCheckBox = view.findViewById(R.id.database_firstName_checkbox);
        mLastNameCheckBox = view.findViewById(R.id.database_lastName_checkbox);
        mMemberCheckBox = view.findViewById(R.id.database_member_checkbox);
        mActiveCheckBox = view.findViewById(R.id.database_active_checkbox);
        mFirstNameEditText = view.findViewById(R.id.database_firstName_editText);
        mLastNameEditText = view.findViewById(R.id.database_lastName_editText);
        mHitsView = view.findViewById(R.id.database_hits);
        mErrorLabel = view.findViewById(R.id.label_error);
        RadioButton sortByFirstNameRadioButton = view.findViewById(R.id.database_sort_first_name_radio_button);
        RadioButton sortByLastNameRadioButton = view.findViewById(R.id.database_sort_last_name_radio_button);

        mPersonAdapter = new PersonAdapter(getContext(), new ArrayList<>(), PersonAdapter.SortMode.FIRST_NAME, view.findViewById(R.id.fixed_header));
        mPersonListView.setAdapter(mPersonAdapter);
        mPersonListView.setOnScrollListener(mPersonAdapter);
        mPersonListView.setOnItemClickListener((parent, view1, position, id) -> showBottomSheetDialogFragment(Objects.requireNonNull(mPersonAdapter.getItem((int) id)).id));

        mSearchButton.setOnClickListener(a -> search());
        sortByFirstNameRadioButton.setOnClickListener(this::onRadioButtonClicked);
        sortByLastNameRadioButton.setOnClickListener(this::onRadioButtonClicked);

        expandCheckBox.setOnCheckedChangeListener(this);

        mFirstNameCheckBox.setOnCheckedChangeListener(this);
        mLastNameCheckBox.setOnCheckedChangeListener(this);
        mMemberCheckBox.setOnCheckedChangeListener(this);
        mActiveCheckBox.setOnCheckedChangeListener(this);

        onCheckedChanged(mFirstNameCheckBox, mFirstNameCheckBox.isChecked());
        onCheckedChanged(mLastNameCheckBox, mLastNameCheckBox.isChecked());
        onCheckedChanged(mMemberCheckBox, mMemberCheckBox.isChecked());
        onCheckedChanged(mActiveCheckBox, mActiveCheckBox.isChecked());
    }

    @Override
    public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.expand_checkBox:
                if (isChecked) {
                    buttonView.setButtonDrawable(R.drawable.ic_arrow_up_accent_animation);
                    ((Animatable) Objects.requireNonNull(buttonView.getButtonDrawable())).start();
                    expand(mExpand);
                } else {
                    buttonView.setButtonDrawable(R.drawable.ic_arrow_down_accent_animation);
                    ((Animatable) Objects.requireNonNull(buttonView.getButtonDrawable())).start();
                    collapse(mExpand);
                }
                break;
            case R.id.database_firstName_checkbox:
                mFirstNameEditText.setEnabled(isChecked);
                if (isChecked) mFirstNameEditText.requestFocus();
                break;
            case R.id.database_lastName_checkbox:
                mLastNameEditText.setEnabled(isChecked);
                if (isChecked) mLastNameEditText.requestFocus();
                break;
        }
    }

    private void onRadioButtonClicked(@NonNull View view) {
        boolean checked = ((RadioButton) view).isChecked();

        switch (view.getId()) {
            case R.id.database_sort_first_name_radio_button:
                if (checked) mSortLastName = false;
                break;
            case R.id.database_sort_last_name_radio_button:
                if (checked) mSortLastName = true;
                break;
        }
    }

    private static void expand(@NonNull final View v) {
        v.setVisibility(View.VISIBLE);
    }

    private static void collapse(@NonNull final View v) {
        v.setVisibility(View.GONE);
    }

    private void search() {
        List<Person> result = new ArrayList<>();

        for (Person person : mPersons) {
            if (mFirstNameCheckBox.isChecked() && !person.firstName.contains(mFirstNameEditText.getText().toString().trim())) continue;
            if (mLastNameCheckBox.isChecked() && !person.lastName.contains(mLastNameEditText.getText().toString().trim())) continue;
            if (mMemberCheckBox.getState() == CheckBoxTriStates.UNCHECKED && person.member) continue;
            else if (mMemberCheckBox.getState() == CheckBoxTriStates.CHECKED && !person.member) continue;
            if (mActiveCheckBox.getState() == CheckBoxTriStates.UNCHECKED && person.active) continue;
            else if (mActiveCheckBox.getState() == CheckBoxTriStates.CHECKED && !person.active) continue;
            result.add(person);
        }

        if (mSortLastName) mPersonAdapter.setSortMode(PersonAdapter.SortMode.LAST_NAME);
        else mPersonAdapter.setSortMode(PersonAdapter.SortMode.FIRST_NAME);

        mPersonAdapter.removeAll(mPersons);
        mPersonAdapter.addAll(result);

        String hits = result.size() + " " + getString(R.string.database_hits);
        mHitsView.post(() -> mHitsView.setText(hits));
    }

    @Override
    public void onPageReceived(String tag, List<Person> persons) {
        this.mPersons.clear();
        mPersonAdapter.clear();

        this.mPersons.addAll(persons);
        mPersonAdapter.addAll(persons);

        if (persons.size() == 0) {
            setError(getString(R.string.database_empty));
        } else {
            mSearchProgress.setVisibility(View.GONE);
            mPersonListView.setVisibility(View.VISIBLE);
            mSearchButton.setEnabled(true);
        }
    }

    @Override
    public void onError(String tag, String reason, Throwable cause) {
        QEDPageReceiver.super.onError(tag, reason, cause);

        final String errorString;

        if (REASON_NETWORK.equals(reason)) {
            errorString = getString(R.string.database_offline);
        } else {
            errorString = getString(R.string.unknown_error);
        }

        setError(errorString);
    }

    private void showBottomSheetDialogFragment(long personId) {
        FragmentManager fragmentManager = getParentFragmentManager();

        int orientation = getResources().getConfiguration().orientation;

        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            PersonBottomSheet personBottomSheet = PersonBottomSheet.newInstance(personId);
            personBottomSheet.show(fragmentManager, personBottomSheet.getTag());
        } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            PersonSideSheet personSideSheet = PersonSideSheet.newInstance(personId);
            personSideSheet.show(fragmentManager, personSideSheet.getTag());
        }
    }

    private void setError(String error) {
        mHandler.post(() -> {
            if (error == null) {
                mErrorLabel.setVisibility(View.GONE);
            } else {
                mErrorLabel.setText(error);
                mErrorLabel.setVisibility(View.VISIBLE);
                mSearchProgress.setVisibility(View.GONE);
                mPersonListView.setVisibility(View.GONE);
            }
        });
    }
}
