package com.jonahbauer.qed.activities.personSheet;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.layoutStuff.FixedHeaderAdapter;
import com.jonahbauer.qed.layoutStuff.viewPagerBottomSheet.ViewPagerBottomSheetFragmentStateAdapter;
import com.jonahbauer.qed.qeddb.event.Event;
import com.jonahbauer.qed.qeddb.person.Person;
import com.jonahbauer.qed.util.Triple;

import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class PersonFragment extends Fragment {
    static final String TAG_PERSON_FRAGMENT = "personFragment";
    static final String ARG_PERSON_ID = "personId";
    static final String ARG_PERSON = "person";

    private ViewPager2 mViewPager;

    @StyleRes
    private int mThemeId;

    public PersonFragment() {}

    public PersonFragment(@StyleRes int themeId) {
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
        PersonFragmentAdapter personFragmentAdapter = new PersonFragmentAdapter(this, getArguments());

        mViewPager = view.findViewById(R.id.pager);
        mViewPager.setAdapter(personFragmentAdapter);

        TabLayout tabLayout = view.findViewById(R.id.tab_layout);

        new TabLayoutMediator(tabLayout, mViewPager,
                (tab, position) -> tab.setText(position == 0 ? getString(R.string.person_tab_personal_data) : (position == 1 ? getString(R.string.person_tab_qed) : getString(R.string.error)))
        ).attach();

        mViewPager.getChildAt(0).setOnTouchListener(new View.OnTouchListener() {
            private float startX, startY;

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, @NonNull MotionEvent event) {
                int action = event.getActionMasked();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getX();
                        startY = event.getY();

                        boolean onLeft = mViewPager.getCurrentItem() == 0;

                        int orientation = getResources().getConfiguration().orientation;

                        if (!onLeft && orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            v.getParent().requestDisallowInterceptTouchEvent(true);
                            return true;
                        }

                        return orientation == Configuration.ORIENTATION_LANDSCAPE;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getX() - startX, dy = event.getY() - startY;
                        if (Math.abs(dx) < 20 && Math.abs(dy) > 20) v.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }

                return v.onTouchEvent(event);
            }
        });
    }

    @NonNull
    private static View addListItem(@NonNull LinearLayout list, @DrawableRes int icon, String title, @StringRes int subtitle) {
        return addListItem(list, icon, title, subtitle != -1 ? list.getContext().getString(subtitle) : null, true);
    }

    @NonNull
    private static View addListItem(@NonNull LinearLayout list, @DrawableRes int icon, String title, String subtitle, boolean drawDivider) {
        Context context = list.getContext();
        assert context != null;

        View view = LayoutInflater.from(context).inflate(R.layout.list_item_title_subtitle, list, false);

        TextView titleView = view.findViewById(R.id.title);
        titleView.setText(title);

        if (subtitle != null) {
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

    private static class PersonFragmentAdapter extends ViewPagerBottomSheetFragmentStateAdapter {
        private final Bundle mArgs;

        PersonFragmentAdapter(Fragment fragment, Bundle args) {
            super(fragment);
            this.mArgs = args;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                Fragment fragment = new PersonTab0();
                fragment.setArguments(mArgs);
                return fragment;
            } else if (position == 1) {
                Fragment fragment = new PersonTab1();
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

    public static class PersonTab0 extends Fragment {
        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_person_tab0, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            Bundle args = getArguments();
            if (args == null) return;

            Person person = args.getParcelable(ARG_PERSON);
            if (person == null) return;

            // Add Name
            String name = person.firstName + " " + person.lastName;
            {
                ((TextView) view.findViewById(R.id.person_name_big)).setText(name);
            }

            LinearLayout list = view.findViewById(R.id.list);

            // Add Phone Numbers
            List<Pair<String, String>> phoneNumbers = person.phoneNumbers;
            {
                int i = 0;
                for (Pair<String, String> phone : phoneNumbers) {
                    View view1 = addListItem(list, i == 0 ? R.drawable.ic_person_phone : -1, phone.second, phone.first, i != 0);
                    view1.setOnClickListener(a -> {
                        Intent intent = new Intent(Intent.ACTION_DIAL);
                        intent.setData(Uri.fromParts("tel", phone.second, null));
                        startActivity(intent);
                    });
                    i++;
                }
            }

            // Add Email
            String email = person.email;
            if (email != null && !email.trim().equals("")) {
                View view1 = addListItem(list, R.drawable.ic_person_mail, email, R.string.person_subtitle_email);
                view1.setOnClickListener(a -> {
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("mailto:"));
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[] {email});
                    intent.putExtra(Intent.EXTRA_SUBJECT, "");
                    startActivity(intent);
                });
            }

            // Add Addresses
            List<String> addresses = person.addresses;
            {
                int i = 0;
                for (String address : addresses) {
                    View view1 = addListItem(list, i == 0 ? R.drawable.ic_person_location : -1, address, -1);
                    view1.setOnClickListener(a -> {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse("geo:0,0?q=" + address));
                        startActivity(intent);
                    });
                    i++;
                }
            }

            // Add Station
            String station = person.homeStation;
            if (station != null && !station.trim().equals("")) {
                addListItem(list, R.drawable.ic_person_train, station, R.string.person_subtitle_station);
            }

            // Add Birthday
            String birthday = person.birthday;
            if (birthday != null && !birthday.trim().equals("")) {
                addListItem(list, R.drawable.ic_person_birthday, birthday, R.string.person_subtitle_birthday);
            }
        }
    }

    public static class PersonTab1 extends Fragment {

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_person_tab1, container, false);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            Bundle args = getArguments();
            if (args == null) return;

            Person person = args.getParcelable(ARG_PERSON);
            if (person == null) return;

            ListView list = view.findViewById(R.id.list);

            List<ListEntry> items = new LinkedList<>();

            // Add Active
            String active = person.active ? getString(R.string.person_active) : getString(R.string.person_inactive);
            {
                items.add(new ListEntry(active, null, -1, R.drawable.ic_person_active));
            }

            // Add Member Since
            String memberSince = person.memberSince;
            if (memberSince != null && !memberSince.trim().equals("")) {
                items.add(new ListEntry(memberSince, null, R.string.person_subtitle_memberSince, R.drawable.ic_person_member_since));
            }

            // Add Management
            List<Triple<String, String, String>> managements = person.management;
            {
                for (Triple<String,String,String> management : managements) {
                    String title = management.first;
                    String subtitle = management.second + " - " + management.third;

                    items.add(new ListEntry(title, subtitle, -1, R.drawable.ic_person_management));
                }
            }

            // Add Events
            List<Pair<Event, String>> events = person.events;
            {
                events.stream().sorted((event1, event2) -> {
                    assert event2.first != null;
                    assert event1.first != null;
                    return event2.first.compareTo(event1.first);
                }).forEachOrdered(event -> {
                    String title;
                    if (event.second != null) title = MessageFormat.format("{0} ({1})", event.first.title, event.second);
                    else title = event.first.title;


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

                    items.add(new ListEntry(title, subtitle, -1, R.drawable.ic_person_event));
                });

                ListAdapter listAdapter = new ListAdapter(list.getContext(), items, view.findViewById(R.id.fixed_header));
                list.setAdapter(listAdapter);
                list.setOnScrollListener(listAdapter);
                list.setOnTouchListener(listAdapter);
            }
        }

        private static class ListEntry {
            private String title;
            private String subtitle;
            private int subtitleResId;
            private int icon;

            ListEntry(String title, String subtitle, int subtitleResId, int icon) {
                this.title = title;
                this.subtitle = subtitle;
                this.subtitleResId = subtitleResId;
                this.icon = icon;
            }
        }

        private class ListAdapter extends FixedHeaderAdapter<ListEntry, Integer> implements View.OnTouchListener {
            ListAdapter(Context context, @NonNull List<ListEntry> itemList, View fixedHeader) {
                super(context, itemList, i -> i.icon, (i, j) -> 0, fixedHeader);
            }

            @NonNull
            @Override
            protected View getItemView(ListEntry object, @Nullable View convertView, @NonNull ViewGroup parent, LayoutInflater inflater) {
                View view;
                if (convertView != null) {
                    view = convertView;
                } else {
                    view = Objects.requireNonNull(inflater).inflate(R.layout.list_item_title_subtitle, parent, false);
                }

                ((ImageView)view.findViewById(R.id.header)).setImageBitmap(null);

                TextView title = view.findViewById(R.id.title);
                TextView subtitle = view.findViewById(R.id.subtitle);

                title.setText(object.title);

                if (object.subtitle != null) {
                    subtitle.setText(object.subtitle);
                    subtitle.setVisibility(View.VISIBLE);
                } else if (object.subtitleResId != -1) {
                    subtitle.setText(object.subtitleResId);
                    subtitle.setVisibility(View.VISIBLE);
                } else {
                    subtitle.setVisibility(View.GONE);
                }

                return view;
            }

            @Override
            protected void setHeader(@NonNull View view, Integer header) {
                ImageView icon = view.findViewById(R.id.header);
                icon.setImageResource(header);
                icon.setVisibility(View.VISIBLE);
            }

            private float startX, startY;
            private boolean onTop = false;

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, @NonNull MotionEvent event) {
                int action = event.getActionMasked();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        if (v instanceof ListView) {
                            ListView list = (ListView) v;
                            View firstView = list.getChildAt(0);

                            onTop = list.getFirstVisiblePosition() == 0 && firstView.getTop() == 0;
                        }

                        startX = event.getX();
                        startY = event.getY();

                        int orientation = getResources().getConfiguration().orientation;

                        if (!onTop || orientation == Configuration.ORIENTATION_LANDSCAPE) v.getParent().requestDisallowInterceptTouchEvent(true);

                        return false;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getX() - startX, dy = event.getY() - startY;
                        if (Math.abs(dx) > 20 && Math.abs(dy) < 20) v.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }

                return v.onTouchEvent(event);
            }
        }
    }
}
