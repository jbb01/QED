package com.jonahbauer.qed.activities.eventSheet;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.layoutStuff.FixedHeaderAdapter;
import com.jonahbauer.qed.layoutStuff.viewPagerBottomSheet.ViewPagerBottomSheetFragmentStateAdapter;
import com.jonahbauer.qed.qeddb.event.Event;
import com.jonahbauer.qed.qeddb.person.Person;

import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public class EventFragment extends Fragment {
    static final String TAG_EVENT_FRAGMENT = "eventFragment";
    static final String ARG_EVENT_ID = "eventId";
    static final String ARG_EVENT = "event";

    @StyleRes
    private int mThemeId;

    public EventFragment() {}

    public EventFragment(@StyleRes int themeId) {
        this.mThemeId = themeId;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (mThemeId != 0) {
            ContextThemeWrapper wrapper = new ContextThemeWrapper(inflater.getContext(), mThemeId);
            inflater.getContext().getTheme().setTo(wrapper.getTheme());
        }
        return inflater.inflate(R.layout.fragment_view_pager, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        EventFragmentAdapter eventFragmentAdapter = new EventFragmentAdapter(this, getArguments());

        ViewPager2 viewPager = view.findViewById(R.id.pager);
        viewPager.setAdapter(eventFragmentAdapter);

        TabLayout tabLayout = view.findViewById(R.id.tab_layout);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(position == 0 ? getString(R.string.event_tab_overview) : (position == 1 ? getString(R.string.event_tab_members) : getString(R.string.error)))
        ).attach();

//        mViewPager.getChildAt(0).setOnTouchListener(new View.OnTouchListener() {
//            private float startX, startY;
//            private boolean onLeft = false;
//
//            @SuppressLint("ClickableViewAccessibility")
//            @Override
//            public boolean onTouch(View v, @NonNull MotionEvent event) {
//                int action = event.getActionMasked();
//                switch (action) {
//                    case MotionEvent.ACTION_DOWN:
//                        startX = event.getX();
//                        startY = event.getY();
//
//                        onLeft = mViewPager.getCurrentItem() == 0;
//
//                        int orientation = getResources().getConfiguration().orientation;
//
//                        if (!onLeft && orientation == Configuration.ORIENTATION_LANDSCAPE) {
//                            v.getParent().requestDisallowInterceptTouchEvent(true);
//                            return true;
//                        }
//
//                        return orientation == Configuration.ORIENTATION_LANDSCAPE;
//                    case MotionEvent.ACTION_MOVE:
//                        float dx = event.getX() - startX, dy = event.getY() - startY;
//                        if (Math.abs(dx) < 20 && Math.abs(dy) > 20) v.getParent().requestDisallowInterceptTouchEvent(false);
//                        break;
//                }
//
//                return v.onTouchEvent(event);
//            }
//        });
    }

    @NonNull
    private static View addListItem(@NotNull LinearLayout list, @DrawableRes int icon, String title, @StringRes int subtitle, boolean drawDivider) {
        Context context = list.getContext();
        assert context != null;

        View view = LayoutInflater.from(context).inflate(R.layout.list_item_title_subtitle, list, false);

        TextView titleView = view.findViewById(R.id.title);
        titleView.setText(title);

        if (subtitle != -1) {
            TextView subtitleView = view.findViewById(R.id.subtitle);
            subtitleView.setText(subtitle);
            subtitleView.setVisibility(View.VISIBLE);
        }

        ImageView imageView = view.findViewById(R.id.header);
        if (icon != -1) {
            imageView.setImageResource(icon);
        } else {
            imageView.setVisibility(View.INVISIBLE);
        }

        if (drawDivider) {
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            ImageView dividerImageView = new ImageView(context);
            dividerImageView.setImageResource(R.drawable.divider);
            LinearLayout.LayoutParams dividerLayoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, displayMetrics)
            );

            list.addView(dividerImageView, dividerLayoutParams);
        }

        list.addView(view);

        return view;
    }

    private static class EventFragmentAdapter extends ViewPagerBottomSheetFragmentStateAdapter {
        private final Bundle mArgs;

        EventFragmentAdapter(Fragment fragment, Bundle args) {
            super(fragment);
            this.mArgs = args;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                Fragment fragment = new EventTab0();
                fragment.setArguments(mArgs);
                return fragment;
            } else if (position == 1) {
                Fragment fragment = new EventTab1();
                fragment.setArguments(mArgs);
                return fragment;
            }
            throw new IndexOutOfBoundsException();
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }

    public static class EventTab0 extends Fragment {
        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_event_tab0, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            Bundle args = getArguments();
            if (args == null) return;

            Event event = args.getParcelable(ARG_EVENT);
            if (event == null) return;

            // Add Name
            String name = event.title;
            if (name != null) {
                ((TextView) view.findViewById(R.id.event_name_big)).setText(name);
            }

            LinearLayout list = view.findViewById(R.id.list);

            // Add Time
            String startString;
            if (event.start != null) {
                startString = MessageFormat.format("{0,date,dd.MM.yyyy}", event.start);
            } else if (event.startString != null) {
                startString = event.startString;
            } else {
                startString = getString(R.string.error);
            }

            String endString;
            if (event.end != null) {
                endString = MessageFormat.format("{0,date,dd.MM.yyyy}", event.end);
            } else if (event.endString != null) {
                endString = event.endString;
            } else {
                endString = getString(R.string.error);
            }

            String time = startString + " - " + endString;
            View timeView = addListItem(list, R.drawable.ic_event_time, time, R.string.event_subtitle_time, false);

            if (event.start != null && event.end != null) {
                timeView.setOnClickListener(a -> {
                    Intent intent = new Intent(Intent.ACTION_INSERT)
                            .setData(CalendarContract.Events.CONTENT_URI)
                            .putExtra(CalendarContract.Events.TITLE, name)
                            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.start.getTime())
                            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.end.getTime())
                            .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true);

                    startActivity(intent);
                });
            }

            // Add Deadline
            if (event.deadline != null) {
                addListItem(list, R.drawable.ic_event_deadline, MessageFormat.format("{0,date,dd.MM.yyyy HH:mm:ss}", event.deadline), R.string.event_subtitle_deadline, true);
            } else if (event.deadlineString != null) {
                addListItem(list, R.drawable.ic_event_deadline, event.deadlineString, R.string.event_subtitle_deadline, true);
            }

            // Add Cost
            int cost = event.cost;
            if (cost != -1) {
                addListItem(list, R.drawable.ic_event_cost, String.valueOf(cost), R.string.event_subtitle_cost, true);
            }

            // Add Max Members
            int maxParticipants = event.maxParticipants;
            if (maxParticipants != -1) {
                addListItem(list, R.drawable.ic_event_max_member, String.valueOf(maxParticipants), R.string.event_subtitle_max_members, true);
            }

            // Add Hotel
            String hotel = event.hotel;
            if (hotel != null && !hotel.trim().equals("")) {
                addListItem(list, R.drawable.ic_event_hotel, hotel, R.string.event_subtitle_hotel, true);
            }


            // Add Organizers
            List<Person> organizers = event.organizers;
            {
                int i = 0;
                for (Person organizer : organizers) {
                    addListItem(list, i == 0 ? R.drawable.ic_event_orga : -1, organizer.firstName + " " + organizer.lastName, R.string.event_subtitle_orga, i == 0);
                    i++;
                }
            }
        }
    }

    public static class EventTab1 extends Fragment {
        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_event_tab1, container, false);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            PersonListAdapter personListAdapter = new PersonListAdapter(
                    getContext(),
                    new ArrayList<>(),
                    person -> person.participationStatus,
                    Comparator.comparing((Person person) -> person.participationStatus).thenComparing(Person.COMPARATOR_FIRST_NAME),
                    view.findViewById(R.id.fixed_header));
            ListView personListView = view.findViewById(R.id.event_member_list);
            personListView.setAdapter(personListAdapter);
            personListView.setOnScrollListener(personListAdapter);

            // TODO personListView.setOnItemClickListener(personListAdapter);

            Bundle args = getArguments();
            if (args == null) return;

            Event event = args.getParcelable(ARG_EVENT);
            if (event == null) return;

            // Add Participants
            personListAdapter.addAll(event.participants);
        }

        private static class PersonListAdapter extends FixedHeaderAdapter<Person, Person.ParticipationStatus> implements SectionIndexer/*, AdapterView.OnItemClickListener, View.OnTouchListener*/ {

            PersonListAdapter(Context context, @NonNull List<Person> itemList, @NonNull Function<Person, Person.ParticipationStatus> headerMap, Comparator<? super Person> comparator, View fixedHeader) {
                super(context, itemList, headerMap, comparator, fixedHeader);
            }

            @NonNull
            @Override
            protected View getItemView(Person person, @Nullable View convertView, @NonNull ViewGroup parent, LayoutInflater inflater) {
                View view = inflater.inflate(R.layout.list_item_title_subtitle, parent, false);

                String name = person.firstName + " " + person.lastName;
                TextView title = view.findViewById(R.id.title);
                TextView subtitle = view.findViewById(R.id.subtitle);

                title.setText(name);
                subtitle.setText(person.email);
                subtitle.setVisibility(View.VISIBLE);

                ((ImageView) view.findViewById(R.id.header)).setImageResource(0);

                return view;
            }

            @Override
            protected void setHeader(@NonNull View view, Person.ParticipationStatus header) {
                switch (header) {
                    case ORGA:
                        ((ImageView) view.findViewById(R.id.header)).setImageResource(R.drawable.ic_event_orga);
                        break;
                    case MEMBER_OPEN:
                        ((ImageView) view.findViewById(R.id.header)).setImageResource(R.drawable.ic_event_member_open);
                        break;
                    case MEMBER_CONFIRMED:
                    case MEMBER_PARTICIPATED:
                        ((ImageView) view.findViewById(R.id.header)).setImageResource(R.drawable.ic_event_member_confirmed);
                        break;
                    case MEMBER_OPT_OUT:
                        ((ImageView) view.findViewById(R.id.header)).setImageResource(R.drawable.ic_event_member_opt_out);
                        break;
                }
            }

            // TODO add on click listener
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                Person person = itemList.get(position);
//
//                if (person != null && getActivity() != null) {
//                    PersonBottomSheet personBottomSheet = PersonBottomSheet.newInstance(String.valueOf(person.id));
//                    if (getFragmentManager() != null) {
//                        personBottomSheet.show(getFragmentManager(), personBottomSheet.getTag());
//                        dismiss();
//                    } else {
//                        Toast.makeText(EventTab1.this.getContext(), R.string.error, Toast.LENGTH_SHORT).show();
//                    }
//                }
//            }

//            private float startX, startY;
//            private boolean onTop = false;
//
//            @SuppressLint("ClickableViewAccessibility")
//            @Override
//            public boolean onTouch(View v, @NonNull MotionEvent event) {
//                int action = event.getActionMasked();
//                switch (action) {
//                    case MotionEvent.ACTION_DOWN:
//                        if (v instanceof ListView) {
//                            ListView list = (ListView) v;
//                            View firstView = list.getChildAt(0);
//
//                            onTop = list.getFirstVisiblePosition() == 0 && firstView.getTop() == 0;
//                        }
//
//                        startX = event.getX();
//                        startY = event.getY();
//
//                        int orientation = getResources().getConfiguration().orientation;
//
//                        if (!onTop || orientation == Configuration.ORIENTATION_LANDSCAPE) v.getParent().requestDisallowInterceptTouchEvent(true);
//
//                        return false;
//                    case MotionEvent.ACTION_MOVE:
//                        float dx = event.getX() - startX, dy = event.getY() - startY;
//                        if (Math.abs(dx) > 20 && Math.abs(dy) < 20) v.getParent().requestDisallowInterceptTouchEvent(false);
//                        break;
//                }
//
//                return v.onTouchEvent(event);
//            }
        }
    }
}
