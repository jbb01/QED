package com.jonahbauer.qed.activities;

import android.content.Context;
import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
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
import androidx.fragment.app.Fragment;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.layoutStuff.CheckBoxTriStates;
import com.jonahbauer.qed.networking.QEDDBPages;
import com.jonahbauer.qed.networking.QEDPageReceiver;
import com.jonahbauer.qed.qeddb.person.Person;
import com.jonahbauer.qed.qeddb.person.PersonAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PersonDatabaseFragment extends Fragment implements CompoundButton.OnCheckedChangeListener, QEDPageReceiver<List<Person>> {
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
    private TextView offlineLabel;
    private Button searchButton;

    static int showPerson;
    static boolean shownPerson;

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
        offlineLabel.setVisibility(View.GONE);
        personListView.setVisibility(View.GONE);
        searchButton.setEnabled(false);
        QEDDBPages.getPersonList(getClass().toString(), this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), Application.darkMode ? R.style.AppTheme_Dark_PersonDatabase : R.style.AppTheme_PersonDatabase);
        inflater = inflater.cloneInContext(contextThemeWrapper);

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
        offlineLabel = view.findViewById(R.id.label_offline);
        RadioButton sortByFirstNameRadioButton = view.findViewById(R.id.database_sort_first_name_radio_button);
        RadioButton sortByLastNameRadioButton = view.findViewById(R.id.database_sort_last_name_radio_button);

        personAdapter = new PersonAdapter(contextThemeWrapper, new ArrayList<>(), PersonAdapter.SORT_FIRST_NAME, view.findViewById(R.id.fixed_header));
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
                    expand(expand, getResources().getInteger(R.integer.expand_time));
                } else {
                    buttonView.setButtonDrawable(R.drawable.ic_arrow_down_accent_animation);
                    ((Animatable) Objects.requireNonNull(buttonView.getButtonDrawable())).start();
                    collapse(expand, getResources().getInteger(R.integer.expand_time));
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

    private static void expand(final View v, int duration) {
        v.setVisibility(View.VISIBLE);
//        v.getLayoutParams().height = 1;
//        v.setVisibility(View.VISIBLE);
//
//        v.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
//        final int targetHeight = v.getMeasuredHeight();
//
//        Animation a = new Animation() {
//            @Override
//            protected void applyTransformation(float interpolatedTime, Transformation t) {
//                v.getLayoutParams().height = interpolatedTime == 1
//                        ? LayoutParams.WRAP_CONTENT
//                        : (int) (targetHeight * interpolatedTime);
//                v.requestLayout();
//            }
//
//            @Override
//            public boolean willChangeBounds() {
//                return true;
//            }
//        };
//
//        a.setDuration(duration);
//        v.startAnimation(a);
    }

    private static void collapse(final View v, int duration) {
        v.setVisibility(View.GONE);
//        final int initialHeight = v.getMeasuredHeight();
//
//        Animation a = new Animation()
//        {
//            @Override
//            protected void applyTransformation(float interpolatedTime, Transformation t) {
//                if(interpolatedTime == 1){
//                    v.setVisibility(View.GONE);
//                }else{
//                    v.getLayoutParams().height = initialHeight - (int)(initialHeight * interpolatedTime);
//                    v.requestLayout();
//                }
//            }
//
//            @Override
//            public boolean willChangeBounds() {
//                return true;
//            }
//        };
//
//        a.setDuration(duration);
//        v.startAnimation(a);
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

        searchProgress.post(() -> {
            searchProgress.setVisibility(View.GONE);
            personListView.setVisibility(View.VISIBLE);
            searchButton.setEnabled(true);
        });

        if (showPerson != 0 && !shownPerson) {
            showBottomSheetDialogFragment(String.valueOf(showPerson));
            shownPerson = true;
        }
    }

    @Override
    public void onNetworkError(String tag) {
        Log.e(Application.LOG_TAG_ERROR, "networkError at " + tag);

        offlineLabel.post(() -> {
            offlineLabel.setVisibility(View.VISIBLE);
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
