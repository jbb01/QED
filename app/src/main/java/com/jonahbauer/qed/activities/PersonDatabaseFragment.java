package com.jonahbauer.qed.activities;

import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.layoutStuff.CheckBoxTriStates;
import com.jonahbauer.qed.networking.QEDDBPages;
import com.jonahbauer.qed.networking.QEDPageReceiver;
import com.jonahbauer.qed.qeddb.person.Person;
import com.jonahbauer.qed.qeddb.person.PersonAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PersonDatabaseFragment extends QEDFragment implements CompoundButton.OnCheckedChangeListener, QEDPageReceiver<List<Person>> {
    private PersonAdapter personAdapter;

    private ListView personListView;
    private ArrayList<Person> persons;
    private View expand;
    private ProgressBar searchProgress;
    private CheckBox firstNameCheckBox;
    private CheckBox lastNameCheckBox;
    private CheckBoxTriStates memberCheckBox;
    private CheckBoxTriStates activeCheckBox;
    private EditText firstNameEditText;
    private EditText lastNameEditText;
    private TextView hitsView;
    private TextView errorLabel;
    private Button searchButton;

    private boolean sortLastName = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        persons = new ArrayList<>();
    }

    @Override
    public void onResume() {
        super.onResume();

        searchProgress.setVisibility(View.VISIBLE);
        errorLabel.setVisibility(View.GONE);
        personListView.setVisibility(View.GONE);
        searchButton.setEnabled(false);
        QEDDBPages.getPersonList(getClass().toString(), this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_persons_database, container, false);

        CheckBox expandCheckBox = view.findViewById(R.id.expand_checkBox);
        personListView = view.findViewById(R.id.person_list_view);
        expand = view.findViewById(R.id.expand);
        searchButton = view.findViewById(R.id.search_button);
        searchProgress = view.findViewById(R.id.search_progress);
        firstNameCheckBox = view.findViewById(R.id.database_firstName_checkbox);
        lastNameCheckBox = view.findViewById(R.id.database_lastName_checkbox);
        memberCheckBox = view.findViewById(R.id.database_member_checkbox);
        activeCheckBox = view.findViewById(R.id.database_active_checkbox);
        firstNameEditText = view.findViewById(R.id.database_firstName_editText);
        lastNameEditText = view.findViewById(R.id.database_lastName_editText);
        hitsView = view.findViewById(R.id.database_hits);
        errorLabel = view.findViewById(R.id.label_error);
        RadioButton sortByFirstNameRadioButton = view.findViewById(R.id.database_sort_first_name_radio_button);
        RadioButton sortByLastNameRadioButton = view.findViewById(R.id.database_sort_last_name_radio_button);

        personAdapter = new PersonAdapter(getContext(), new ArrayList<>(), PersonAdapter.SORT_FIRST_NAME, view.findViewById(R.id.fixed_header));
        personListView.setAdapter(personAdapter);
        personListView.setOnScrollListener(personAdapter);
        personListView.setOnItemClickListener((parent, view1, position, id) -> showBottomSheetDialogFragment(String.valueOf(Objects.requireNonNull(personAdapter.getItem((int) id)).id)));

        searchButton.setOnClickListener(a -> search());
        sortByFirstNameRadioButton.setOnClickListener(this::onRadioButtonClicked);
        sortByLastNameRadioButton.setOnClickListener(this::onRadioButtonClicked);

        expandCheckBox.setOnCheckedChangeListener(this);

        firstNameCheckBox.setOnCheckedChangeListener(this);
        lastNameCheckBox.setOnCheckedChangeListener(this);
        memberCheckBox.setOnCheckedChangeListener(this);
        activeCheckBox.setOnCheckedChangeListener(this);

        onCheckedChanged(firstNameCheckBox, firstNameCheckBox.isChecked());
        onCheckedChanged(lastNameCheckBox, lastNameCheckBox.isChecked());
        onCheckedChanged(memberCheckBox, memberCheckBox.isChecked());
        onCheckedChanged(activeCheckBox, activeCheckBox.isChecked());

        return view;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.expand_checkBox:
                if (isChecked) {
                    buttonView.setButtonDrawable(R.drawable.ic_arrow_up_accent_animation);
                    ((Animatable) Objects.requireNonNull(buttonView.getButtonDrawable())).start();
                    expand(expand);
                } else {
                    buttonView.setButtonDrawable(R.drawable.ic_arrow_down_accent_animation);
                    ((Animatable) Objects.requireNonNull(buttonView.getButtonDrawable())).start();
                    collapse(expand);
                }
                break;
            case R.id.database_firstName_checkbox:
                firstNameEditText.setEnabled(isChecked);
                if (isChecked) firstNameEditText.requestFocus();
                break;
            case R.id.database_lastName_checkbox:
                lastNameEditText.setEnabled(isChecked);
                if (isChecked) lastNameEditText.requestFocus();
                break;
        }
    }

    private void onRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();

        switch (view.getId()) {
            case R.id.database_sort_first_name_radio_button:
                if (checked) sortLastName = false;
                break;
            case R.id.database_sort_last_name_radio_button:
                if (checked) sortLastName = true;
                break;
        }
    }

    private static void expand(final View v) {
        v.setVisibility(View.VISIBLE);
    }

    private static void collapse(final View v) {
        v.setVisibility(View.GONE);
    }

    private void search() {
        List<Person> result = new ArrayList<>();

        for (Person person : persons) {
            if (firstNameCheckBox.isChecked() && !person.firstName.contains(firstNameEditText.getText().toString().trim())) continue;
            if (lastNameCheckBox.isChecked() && !person.lastName.contains(lastNameEditText.getText().toString().trim())) continue;
            if (memberCheckBox.getState() == CheckBoxTriStates.UNCHECKED && person.member) continue;
            else if (memberCheckBox.getState() == CheckBoxTriStates.CHECKED && !person.member) continue;
            if (activeCheckBox.getState() == CheckBoxTriStates.UNCHECKED && person.active) continue;
            else if (activeCheckBox.getState() == CheckBoxTriStates.CHECKED && !person.active) continue;
            result.add(person);
        }

        if (sortLastName) personAdapter.setSort(PersonAdapter.SORT_LAST_NAME);
        else personAdapter.setSort(PersonAdapter.SORT_FIRST_NAME);

        personAdapter.removeAll(persons);
        personAdapter.addAll(result);

        String hits = result.size() + " " + getString(R.string.database_hits);
        hitsView.post(() -> hitsView.setText(hits));
    }

    @Override
    public void onPageReceived(String tag, List<Person> persons) {
        this.persons.clear();
        personAdapter.clear();

        this.persons.addAll(persons);
        personAdapter.addAll(persons);

        handler.post(() -> {
            searchProgress.setVisibility(View.GONE);
            personListView.setVisibility(View.VISIBLE);
            searchButton.setEnabled(true);
        });
    }

    @Override
    public void onError(String tag, String reason, Throwable cause) {
        QEDPageReceiver.super.onError(tag, reason, cause);

        final String errorString;

        if (REASON_NETWORK.equals(reason))
            errorString = getString(R.string.database_offline);
        else
            errorString = getString(R.string.unknown_error);

        handler.post(() -> {
            errorLabel.setText(errorString);
            errorLabel.setVisibility(View.VISIBLE);
            searchProgress.setVisibility(View.GONE);
            personListView.setVisibility(View.GONE);
        });

//        Intent intent = new Intent(getActivity(), LoginActivity.class);
//        intent.putExtra(LoginActivity.ERROR_MESSAGE, getString(R.string.database_login_failed));
//        startActivity(intent);
//        if (getActivity() != null) getActivity().finish();

//        searchProgress.setVisibility(View.GONE);
//        personListView.setVisibility(View.VISIBLE);
//
//        Toast.makeText(getContext(), R.string.error, Toast.LENGTH_LONG).show();
    }

    private void showBottomSheetDialogFragment(String personId) {
        assert getFragmentManager() != null;

        PersonBottomSheet personBottomSheet = PersonBottomSheet.newInstance(personId);
        personBottomSheet.show(getFragmentManager(), personBottomSheet.getTag());
    }
}
