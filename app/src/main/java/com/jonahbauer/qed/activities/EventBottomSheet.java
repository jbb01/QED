package com.jonahbauer.qed.activities;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SectionIndexer;
import android.widget.TabHost;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.layoutStuff.AnimatedTabHostListener;
import com.jonahbauer.qed.layoutStuff.FixedHeaderAdapter;
import com.jonahbauer.qed.qeddb.QEDDBEvent;
import com.jonahbauer.qed.qeddb.QEDDBEventReceiver;
import com.jonahbauer.qed.qeddb.event.Event;
import com.jonahbauer.qed.qeddb.person.Person;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import static com.jonahbauer.qed.qeddb.person.Person.MemberType.MEMBER_CONFIRMED;
import static com.jonahbauer.qed.qeddb.person.Person.MemberType.MEMBER_OPEN;
import static com.jonahbauer.qed.qeddb.person.Person.MemberType.MEMBER_OPT_OUT;
import static com.jonahbauer.qed.qeddb.person.Person.MemberType.ORGA;

public class EventBottomSheet extends BottomSheetDialogFragment implements QEDDBEventReceiver {
    public static final String EXTRA_SESSION_ID = "sessionId";
    public static final String EXTRA_EVENT_ID = "eventId";
    public static final String EXTRA_COOKIE = "cookie";

    private static Comparator<Person> comparator = (p1, p2) -> {
        if (p1.type != p2.type) return p1.type.compareTo(p2.type);
        else {
            String name1 = p1.firstName + " " + p1.lastName;
            String name2 = p2.firstName + " " + p2.lastName;
            return name1.compareTo(name2);
        }
    };

    private Event event;

    private ProgressBar progressBar;
    private ViewGroup tabcontent;
    private TextView nameBigTextView;
    private TextView timeTextView;
    private TextView deadlineTextView;
    private TextView costTextView;
    private TextView maxMembersTextView;
    private TextView hotelTextView;
    private TableRow hotelTableRow;
    private TableRow orgaTableRow;
    private TableRow timeTableRow;
    private LinearLayout orgaLinearLayout;

    private PersonListAdapter personListAdapter;

    public static EventBottomSheet newInstance(char[] sessionId, char[] cookie, String eventId) {
        Bundle args = new Bundle();
        args.putCharArray(EXTRA_SESSION_ID, sessionId);
        args.putCharArray(EXTRA_COOKIE, cookie);
        args.putString(EXTRA_EVENT_ID, eventId);
        EventBottomSheet fragment = new EventBottomSheet();
        fragment.setArguments(args);
        return fragment;
    }

    public static EventBottomSheet newInstance(char[] sessionId, char[] cookie, Event event) {
        Bundle args = new Bundle();
        args.putCharArray(EXTRA_SESSION_ID, sessionId);
        args.putCharArray(EXTRA_COOKIE, cookie);
        args.putString(EXTRA_EVENT_ID, event.id);
        EventBottomSheet fragment = new EventBottomSheet();
        fragment.event = event;
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        assert args != null;
        char[] sessionId = args.getCharArray(EXTRA_SESSION_ID);
        String eventId = args.getString(EXTRA_EVENT_ID);
        char[] cookie = args.getCharArray(EXTRA_COOKIE);

        args.putCharArray(EXTRA_SESSION_ID, new char[0]);
        args.putCharArray(EXTRA_COOKIE, new char[0]);

        QEDDBEvent qeddbEvent = new QEDDBEvent();
        qeddbEvent.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this, sessionId, cookie, event != null ? event : eventId);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_event, container);

        progressBar = view.findViewById(R.id.progress_bar);
        tabcontent = view.findViewById(android.R.id.tabcontent);
        TabHost tabHost = view.findViewById(R.id.tabHost);

        nameBigTextView = view.findViewById(R.id.event_name_big);
        timeTextView = view.findViewById(R.id.event_time_text);
        timeTableRow = view.findViewById(R.id.event_time_row);
        deadlineTextView = view.findViewById(R.id.event_deadline_text);
        costTextView = view.findViewById(R.id.event_cost_text);
        maxMembersTextView = view.findViewById(R.id.event_max_member_text);
        hotelTextView = view.findViewById(R.id.event_hotel_text);
        hotelTableRow = view.findViewById(R.id.event_hotel_row);
        orgaTableRow = view.findViewById(R.id.event_orga_row);
        orgaLinearLayout = view.findViewById(R.id.event_orga_list);

        personListAdapter = new PersonListAdapter(getContext(), new ArrayList<>(), person -> person.type, comparator, view.findViewById(R.id.fixed_header));
        ListView personListView = view.findViewById(R.id.event_member_list);
        personListView.setAdapter(personListAdapter);
        personListView.setOnScrollListener(personListAdapter);
        personListView.setOnItemClickListener(personListAdapter);

        tabHost.setup();

        //Tab 1
        TabHost.TabSpec spec = tabHost.newTabSpec(getString(R.string.event_tab_overview));
        spec.setContent(R.id.tab1);
        spec.setIndicator(getString(R.string.event_tab_overview));
        tabHost.addTab(spec);

        //Tab 2
        spec = tabHost.newTabSpec(getString(R.string.event_tab_members));
        spec.setContent(R.id.tab2);
        spec.setIndicator(getString(R.string.event_tab_members));
        tabHost.addTab(spec);

        tabHost.setOnTabChangedListener(new AnimatedTabHostListener(getContext(), tabHost));

        return view;
    }


    @Override
    public void onEventReceived(Event event) {
        if (event == null) {
            Toast.makeText(getContext(), R.string.no_internet, Toast.LENGTH_SHORT).show();
            this.dismiss();
            return;
        }

        nameBigTextView.setText(event.name);
        if (event.start != null && event.end != null) {
            timeTextView.setText(MessageFormat.format("{0,date,dd.MM.yyyy} - {1,date,dd.MM.yyyy}", event.start, event.end));
            timeTableRow.setOnClickListener(a -> {
                Intent intent = new Intent(Intent.ACTION_INSERT)
                        .setData(CalendarContract.Events.CONTENT_URI)
                        .putExtra(CalendarContract.Events.TITLE, event.name)
                        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.start.getTime())
                        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.end.getTime())
                        .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true);

                startActivity(intent);
            });
        } else
            timeTextView.setText(MessageFormat.format("{0} - {1}", event.startString, event.endString));
        if (event.deadline != null)
            deadlineTextView.setText(MessageFormat.format("{0,date,dd.MM.yyyy HH:mm:ss}", event.deadline));
        else
            deadlineTextView.setText(event.deadlineString);
        costTextView.setText(event.cost);
        maxMembersTextView.setText(event.maxMember);
        if (event.hotel != null) {
            hotelTableRow.setVisibility(View.VISIBLE);
            hotelTextView.setText(event.hotel);
        }

        for (Person person : event.organizer) {
            addListItem(orgaLinearLayout, orgaTableRow, person.firstName + " " + person.lastName, "Orga");
        }

        personListAdapter.addAll(event.members);

        progressBar.setVisibility(View.GONE);
        tabcontent.setVisibility(View.VISIBLE);
    }

    public View addListItem(LinearLayout list, TableRow row, String title, String subtitle) {
        row.setVisibility(View.VISIBLE);

        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        assert getContext() != null;

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

    private class PersonListAdapter extends FixedHeaderAdapter<Person, Person.MemberType> implements SectionIndexer, AdapterView.OnItemClickListener {

        PersonListAdapter(Context context, @NonNull List<Person> itemList, @NonNull Function<Person, Person.MemberType> headerMap, Comparator<? super Person> comparator, View fixedHeader) {
            super(context, itemList, headerMap, comparator, fixedHeader);
        }

        @NonNull
        @Override
        protected View getItemView(Person person, @Nullable View convertView, @NonNull ViewGroup parent, LayoutInflater inflater) {
            View view = inflater.inflate(R.layout.list_item_member, parent, false);

            String name = person.firstName + " " + person.lastName;
            ((TextView)view.findViewById(R.id.event_member_name)).setText(name);
            ((TextView)view.findViewById(R.id.event_member_email)).setText(person.email);

            ((ImageView)view.findViewById(R.id.header)).setImageResource(0);

            return view;
        }

        @Override
        protected void setHeader(@NonNull View view, Person.MemberType header) {
            if (header == ORGA)
                ((ImageView)view.findViewById(R.id.header)).setImageResource(R.drawable.ic_event_orga);
            else if (header == MEMBER_OPEN)
                ((ImageView)view.findViewById(R.id.header)).setImageResource(R.drawable.ic_event_member_open);
            else if (header == MEMBER_CONFIRMED)
                ((ImageView)view.findViewById(R.id.header)).setImageResource(R.drawable.ic_event_member_confirmed);
            else if (header == MEMBER_OPT_OUT)
                ((ImageView)view.findViewById(R.id.header)).setImageResource(R.drawable.ic_event_member_opt_out);
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Person person = itemList.get(position);

            if (person != null && getActivity() != null) {
                PersonDatabaseFragment.showPerson = person.id;
                PersonDatabaseFragment.shownPerson = false;
                getContext().getSharedPreferences(getString(R.string.preferences_shared_preferences), Context.MODE_PRIVATE).edit().putInt(getString(R.string.preferences_drawerSelection_key), R.id.nav_database_persons).apply();
                ((MainActivity)getActivity()).reloadFragment(false);
                dismiss();
            }
        }
    }
}
