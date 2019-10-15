package com.jonahbauer.qed.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TabHost;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.layoutStuff.AnimatedTabHostListener;
import com.jonahbauer.qed.qeddb.QEDDBPerson;
import com.jonahbauer.qed.qeddb.QEDDBPersonReceiver;
import com.jonahbauer.qed.qeddb.event.Event;
import com.jonahbauer.qed.qeddb.person.Person;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class PersonBottomSheet extends BottomSheetDialogFragment implements QEDDBPersonReceiver {
    public static final String EXTRA_SESSION_ID = "sessionId";
    public static final String EXTRA_PERSON_ID = "personId";
    public static final String EXTRA_COOKIE = "cookie";

    private float dp;

    private TextView nameBigTextView;
    private TextView activeTextView;
    private TextView memberSinceTextView;
    private TableRow phoneTableRow;
    private TableRow mailTableRow;
    private TableRow addressTableRow;
    private TableRow stationTableRow;
    private TableRow birthdayTableRow;
    private TableRow managementTableRow;
    private LinearLayout phoneLinearLayout;
    private LinearLayout mailLinearLayout;
    private LinearLayout addressLinearLayout;
    private LinearLayout stationLinearLayout;
    private LinearLayout birthdayLinearLayout;
    private LinearLayout managementLinearLayout;

    private EventListAdapter eventListAdapter;

    private ProgressBar progressBar;
    private ViewGroup tabcontent;

    public static PersonBottomSheet newInstance(char[] sessionId, char[] cookie, String personId) {
        Bundle args = new Bundle();
        args.putCharArray(EXTRA_SESSION_ID, sessionId);
        args.putCharArray(EXTRA_COOKIE, cookie);
        args.putString(EXTRA_PERSON_ID, personId);
        PersonBottomSheet fragment = new PersonBottomSheet();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        assert args != null;
        char[] sessionId = args.getCharArray(EXTRA_SESSION_ID);
        String personId = args.getString(EXTRA_PERSON_ID);
        char[] cookie = args.getCharArray(EXTRA_COOKIE);

        args.putCharArray(EXTRA_SESSION_ID, new char[0]);
        args.putCharArray(EXTRA_COOKIE, new char[0]);

        QEDDBPerson qeddbPerson = new QEDDBPerson();
        qeddbPerson.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this, sessionId, cookie, personId);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_person, container);

        dp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());

        progressBar = view.findViewById(R.id.progress_bar);
        tabcontent = view.findViewById(android.R.id.tabcontent);
        TabHost tabHost = view.findViewById(R.id.tabHost);

        nameBigTextView = view.findViewById(R.id.person_name_big);
        memberSinceTextView = view.findViewById(R.id.person_memberSince_text);
        activeTextView = view.findViewById(R.id.person_active_text);
        phoneTableRow = view.findViewById(R.id.person_phone_row);
        mailTableRow = view.findViewById(R.id.person_mail_row);
        addressTableRow = view.findViewById(R.id.person_address_row);
        stationTableRow = view.findViewById(R.id.person_station_row);
        birthdayTableRow = view.findViewById(R.id.person_birthday_row);
        managementTableRow = view.findViewById(R.id.person_management_row);
        phoneLinearLayout = view.findViewById(R.id.person_phone_list);
        mailLinearLayout = view.findViewById(R.id.person_mail_list);
        addressLinearLayout = view.findViewById(R.id.person_address_list);
        stationLinearLayout = view.findViewById(R.id.person_station_list);
        birthdayLinearLayout = view.findViewById(R.id.person_birthday_list);
        managementLinearLayout = view.findViewById(R.id.person_management_list);
        ListView eventListView = view.findViewById(R.id.person_events_list);

        eventListAdapter = new EventListAdapter(view.getContext(), new ArrayList<>());
        eventListView.setAdapter(eventListAdapter);
        eventListView.setOnItemClickListener(eventListAdapter);

        tabHost.setup();

        //Tab 1
        TabHost.TabSpec spec = tabHost.newTabSpec(getString(R.string.person_tab_personal_data));
        spec.setContent(R.id.tab1);
        spec.setIndicator(getString(R.string.person_tab_personal_data));
        tabHost.addTab(spec);

        //Tab 2
        spec = tabHost.newTabSpec(getString(R.string.person_tab_qed));
        spec.setContent(R.id.tab2);
        spec.setIndicator(getString(R.string.person_tab_qed));
        tabHost.addTab(spec);

        tabHost.setOnTabChangedListener(new AnimatedTabHostListener(getContext(), tabHost));

        return view;
    }


    @Override
    public void onPersonReceived(Person person) {
        if (person == null) {
            Toast.makeText(getContext(), R.string.no_internet, Toast.LENGTH_SHORT).show();
            this.dismiss();
            return;
        }

        for (Pair<String, String> phone : person.phoneNumbers) {
            View view = addListItem(phoneLinearLayout, phoneTableRow, phone.second, phone.first);
            view.setOnClickListener(a -> {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.fromParts("tel", phone.second, null));
                startActivity(intent);
            });
        }

        if (person.email != null && !person.email.trim().equals("")) {
            View view = addListItem(mailLinearLayout, mailTableRow, person.email, getString(R.string.person_subtitle_email));
            view.setOnClickListener(a -> {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:"));
                intent.putExtra(Intent.EXTRA_EMAIL, new String[] {person.email});
                intent.putExtra(Intent.EXTRA_SUBJECT, "");
                startActivity(intent);
            });
        }

        for (String address : person.addresses) {
            View view = addListItem(addressLinearLayout, addressTableRow, address, null);
            view.setOnClickListener(a -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("geo:0,0?q=" + address));
                startActivity(intent);
            });
        }

        if (person.homeStation != null && !person.homeStation.trim().equals("")) addListItem(stationLinearLayout, stationTableRow, person.homeStation, getString(R.string.person_subtitle_station));

        if (person.birthday != null && !person.birthday.trim().equals("") && !person.birthday.trim().equals("00.00.0000")) addListItem(birthdayLinearLayout, birthdayTableRow, person.birthday, getString(R.string.person_subtitle_birthday));

        memberSinceTextView.setText(person.memberSince);
        activeTextView.setText(person.active ? getString(R.string.person_active) : getString(R.string.person_inactive));
        person.events.stream().sorted((event1, event2) -> {
            assert event2.first != null;
            assert event1.first != null;
            return event2.first.compareTo(event1.first);
        }).forEachOrdered(eventListAdapter::add);
        if (person.events.size() == 0) eventListAdapter.add(getString(R.string.person_no_events), null);
        eventListAdapter.notifyDataSetChanged();

        for (Pair<String,Pair<String,String>> management : person.management) {
            assert management.second != null;
            String title = management.first;
            String subtitle = management.second.first + " - " + management.second.second;
            addListItem(managementLinearLayout, managementTableRow, title, subtitle);
        }

        String name = person.firstName + " " + person.lastName;
        nameBigTextView.setText(name);

        progressBar.setVisibility(View.GONE);
        tabcontent.setVisibility(View.VISIBLE);
    }

    public View addListItem(LinearLayout list, TableRow row, String title, String subtitle) {
        row.setVisibility(View.VISIBLE);

        assert getContext() != null;

        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        TextView titleTextView = new TextView(getContext());
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        titleTextView.setTextColor(getContext().getColor(android.R.color.black));
        titleTextView.setText(title);
        linearLayout.addView(titleTextView);

        if (subtitle != null) {
            TextView subtitleTextView = new TextView(getContext());
            subtitleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            subtitleTextView.setText(subtitle);
            linearLayout.addView(subtitleTextView);
        }

        ImageView dividerImageView = new ImageView(getContext());
        dividerImageView.setImageResource(R.drawable.person_divider);
        LinearLayout.LayoutParams dividerLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics())
        );
        dividerLayoutParams.setMargins(
                0,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics()),
                0,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics())
        );
        linearLayout.addView(dividerImageView, dividerLayoutParams);

        list.addView(linearLayout);

        return linearLayout;
    }

    private class EventListAdapter extends ArrayAdapter<Pair<Event,String>> implements AdapterView.OnItemClickListener {
        private List<Pair<Event, String>> eventList;
        private Context context;

        EventListAdapter(Context context, @NonNull List<Pair<Event,String>> eventList) {
            super(context, 0, eventList);
            this.eventList = eventList;
            this.context = context;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setOrientation(LinearLayout.VERTICAL);

            if (position == 0)
                linearLayout.setPadding(0, 0, 0, (int) (5 * dp));
            else
                linearLayout.setPadding(0, (int) (5 * dp), 0, (int) (5 * dp));

            Pair<Event, String> event = eventList.get(position);

            assert event.first != null;

            String title;
            if (event.second != null) title = MessageFormat.format("{0} ({1})", event.first.name, event.second);
            else title = event.first.name;


            String subtitle;
            if ((event.first.start != null || event.first.startString != null) && (event.first.end != null || event.first.endString != null))
                subtitle = MessageFormat.format(
                        ((event.first.start != null) ? "{0,date,dd.MM.yyyy}" : "{0}") + " - " +
                                ((event.first.end != null) ? "{1,date,dd.MM.yyyy}" : "{1}"),
                                (event.first.start != null) ? event.first.start : event.first.startString,
                                (event.first.end != null) ? event.first.end : event.first.endString
                );
            else if (event.first.start != null || event.first.startString != null)
                subtitle = MessageFormat.format(
                        (event.first.start != null) ? "{0,date,dd.MM.yyyy}" : "{0}",
                        (event.first.start != null) ? event.first.start : event.first.startString
                );
            else if (event.first.end != null || event.first.endString != null)
                subtitle = MessageFormat.format(
                        (event.first.end != null) ? "{0,date,dd.MM.yyyy}" : "{0}",
                        (event.first.end != null) ? event.first.end : event.first.endString
                );
            else subtitle = "";


            TextView titleTextView = new TextView(getContext());
            titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            titleTextView.setTextColor(getContext().getColor(android.R.color.black));
            titleTextView.setText(title);
            linearLayout.addView(titleTextView);

            TextView subtitleTextView = new TextView(getContext());
            subtitleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            subtitleTextView.setText(subtitle);
            linearLayout.addView(subtitleTextView);

            linearLayout.setEnabled(false);

            return linearLayout;
        }

        public void add(String title, String subtitle) {
            Event event = new Event();
            event.name = title;
            event.startString = subtitle;
            add(new Pair<>(event, null));
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Event event = eventList.get(position).first;

            if (event != null && getActivity() != null) {
                EventDatabaseFragment.showEventId = 0;
                EventDatabaseFragment.showEvent = event.name;
                EventDatabaseFragment.shownEvent = false;
                getContext().getSharedPreferences(getString(R.string.preferences_shared_preferences), Context.MODE_PRIVATE).edit().putInt(getString(R.string.preferences_drawerSelection_key), R.id.nav_database_events).apply();
                ((MainActivity)getActivity()).reloadFragment(false);
                dismiss();
            }
        }
    }
}
