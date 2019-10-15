package com.jonahbauer.qed.activities;

import android.content.Intent;
import android.graphics.drawable.Animatable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TableRow.LayoutParams;
import android.widget.TextView;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.layoutStuff.CheckBoxTriStates;
import com.jonahbauer.qed.qeddb.QEDDBLogin;
import com.jonahbauer.qed.qeddb.QEDDBLoginReceiver;
import com.jonahbauer.qed.qeddb.QEDDBPersonsList;
import com.jonahbauer.qed.qeddb.QEDDBPersonsListReceiver;
import com.jonahbauer.qed.qeddb.person.Person;
import com.jonahbauer.qed.qeddb.person.PersonAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PersonDatabaseFragment extends Fragment implements CompoundButton.OnCheckedChangeListener, QEDDBLoginReceiver, QEDDBPersonsListReceiver {
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
    private char[] sessionId;
    private char[] cookie;

    private boolean sortLastName = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        persons = new ArrayList<>();
        QEDDBLogin qeddbLogin = new QEDDBLogin();
        qeddbLogin.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_persons_database, container, false);

        CheckBox expandCheckBox = view.findViewById(R.id.expand_checkBox);
        personListView = view.findViewById(R.id.person_list_view);
        expand = view.findViewById(R.id.expand);
        Button searchButton = view.findViewById(R.id.search_button);
        searchProgress = view.findViewById(R.id.search_progress);
        firstNameCheckBox = view.findViewById(R.id.database_firstName_checkbox);
        lastNameCheckBox = view.findViewById(R.id.database_lastName_checkbox);
        memberCheckBox = view.findViewById(R.id.database_member_checkbox);
        activeCheckBox = view.findViewById(R.id.database_active_checkbox);
        firstNameEditText = view.findViewById(R.id.database_firstName_editText);
        lastNameEditText = view.findViewById(R.id.database_lastName_editText);
        hitsView = view.findViewById(R.id.database_hits);
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
    public void onDestroy() {
        if (sessionId != null) for (int i = 0; i < sessionId.length; i++) sessionId[i] = 0;
        if (cookie != null) for (int i = 0; i < cookie.length; i++) cookie[i] = 0;
        super.onDestroy();
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

    public void onRadioButtonClicked(View view) {
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
        v.getLayoutParams().height = 1;
        v.setVisibility(View.VISIBLE);

        v.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        final int targetHeight = v.getMeasuredHeight();

        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                v.getLayoutParams().height = interpolatedTime == 1
                        ? LayoutParams.WRAP_CONTENT
                        : (int) (targetHeight * interpolatedTime);
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setDuration(duration);
        v.startAnimation(a);
    }

    private static void collapse(final View v, int duration) {
        final int initialHeight = v.getMeasuredHeight();

        Animation a = new Animation()
        {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if(interpolatedTime == 1){
                    v.setVisibility(View.GONE);
                }else{
                    v.getLayoutParams().height = initialHeight - (int)(initialHeight * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setDuration(duration);
        v.startAnimation(a);
    }

    public void search() {
        Stream<Person> stream = persons.stream();
        if (firstNameCheckBox.isChecked()) stream = stream.filter(person -> person.firstName.contains(firstNameEditText.getText().toString().trim()));
        if (lastNameCheckBox.isChecked()) stream = stream.filter(person -> person.lastName.contains(lastNameEditText.getText().toString().trim()));
        if (memberCheckBox.getState() == CheckBoxTriStates.UNCHECKED) stream = stream.filter(person -> !person.member);
        else if (memberCheckBox.getState() == CheckBoxTriStates.CHECKED) stream = stream.filter(person -> person.member);
        if (activeCheckBox.getState() == CheckBoxTriStates.UNCHECKED) stream = stream.filter(person -> !person.active);
        else if (activeCheckBox.getState() == CheckBoxTriStates.CHECKED) stream = stream.filter(person -> person.active);

        List<Person> result = stream.collect(Collectors.toList());

        if (sortLastName) personAdapter.setSort(PersonAdapter.SORT_LAST_NAME);
        else personAdapter.setSort(PersonAdapter.SORT_FIRST_NAME);

        personAdapter.removeAll(persons);
        personAdapter.addAll(result);

        String hits = result.size() + " " + getString(R.string.database_hits);
        hitsView.post(() -> hitsView.setText(hits));
    }

    @Override
    public void onReceiveSessionId(char[] sessionId, char[] cookie) {
        if (sessionId == null) {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.putExtra(LoginActivity.ERROR_MESSAGE, getString(R.string.database_login_failed));
            startActivity(intent);
            if (getActivity() != null) getActivity().finish();
        } else {
            this.sessionId = Arrays.copyOf(sessionId, sessionId.length);
            this.cookie = Arrays.copyOf(cookie, cookie.length);
            QEDDBPersonsList qeddbPersons = new QEDDBPersonsList();
            qeddbPersons.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this, sessionId, cookie);
        }
    }

    @Override
    public void onPersonsListReceived(List<Person> persons) {
        this.persons.addAll(persons);
        personAdapter.addAll(persons);
        searchProgress.setVisibility(View.GONE);
        personListView.setVisibility(View.VISIBLE);
    }

    public void showBottomSheetDialogFragment(String personId) {
        assert getFragmentManager() != null;

        PersonBottomSheet personBottomSheet = PersonBottomSheet.newInstance(sessionId, cookie, personId);
        personBottomSheet.show(getFragmentManager(), personBottomSheet.getTag());
    }
}
