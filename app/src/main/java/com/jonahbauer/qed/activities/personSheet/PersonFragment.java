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
import com.jonahbauer.qed.model.Person;
import com.jonahbauer.qed.model.Registration;

import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PersonFragment extends Fragment {
    static final String TAG_PERSON_FRAGMENT = "personFragment";
    static final String ARG_PERSON = "person";
    static final String ARG_FETCH_DATA = "fetchData";

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
    private static View addListItem(@NonNull LinearLayout list, @DrawableRes int icon, String title, @StringRes int subtitle, boolean drawDivider) {
        return addListItem(list, icon, title, subtitle != -1 ? list.getContext().getString(subtitle) : null, drawDivider);
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
            String name = person.getFirstName() + " " + person.getLastName();
            {
                ((TextView) view.findViewById(R.id.person_name_big)).setText(name);
            }

            LinearLayout list = view.findViewById(R.id.list);
            int listIndex = 0;

            // Add Email
            String email = person.getEmail();
            if (email != null && !email.trim().equals("")) {
                View view1 = addListItem(list, R.drawable.ic_person_mail, email, R.string.person_subtitle_email, listIndex++ != 0);
                view1.setOnClickListener(a -> {
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[] {email});
                    if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                        startActivity(intent);
                    }
                });
            }

            // Add Contacts
            List<Pair<String, String>> phoneNumbers = person.getContacts();
            int i = 0;
            for (Pair<String, String> phone : phoneNumbers) {
                View view1 = addListItem(list, (i++ == 0) ? R.drawable.ic_person_contact : -1, phone.second, phone.first, listIndex++ != 0);

                if (phone.second.matches("[0-9 _+\\-/()]*")) {
                    view1.setOnClickListener(a -> {
                        Intent intent = new Intent(Intent.ACTION_DIAL);
                        intent.setData(Uri.fromParts("tel", phone.second, null));
                        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                            startActivity(intent);
                        }
                    });
                }
            }

            // Add Addresses
            List<String> addresses = person.getAddresses();
            int j = 0;
            for (String address : addresses) {
                View view1 = addListItem(list, (j++ == 0) ? R.drawable.ic_person_location : -1, address, -1, listIndex++ != 0);
                view1.setOnClickListener(a -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("geo:0,0?q=" + address));
                    if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                        startActivity(intent);
                    }
                });
            }

            // Add Station
            String station = person.getHomeStation();
            if (station != null && !station.trim().equals("")) {
                addListItem(list, R.drawable.ic_person_train, station, R.string.person_subtitle_station, listIndex++ != 0);
            }

            // Add Birthday
            if (person.getBirthday() != null) {
                addListItem(list, R.drawable.ic_person_birthday, MessageFormat.format("{0,date}", person.getBirthday()), R.string.person_subtitle_birthday, listIndex++ != 0);
            } else if (person.getBirthdayString() != null && !person.getBirthdayString().trim().equals("")) {
                addListItem(list, R.drawable.ic_person_birthday, person.getBirthdayString(), R.string.person_subtitle_birthday, listIndex++ != 0);
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
            String active = person.isActive() ? getString(R.string.person_active) : getString(R.string.person_inactive);
            {
                items.add(new ListEntry(active, null, -1, R.drawable.ic_person_active));
            }

            // Add Member Since
            if (person.getMemberSince() != null) {
                items.add(new ListEntry(MessageFormat.format("{0,date}", person.getMemberSince()), null, R.string.person_subtitle_memberSince, R.drawable.ic_person_member_since));
            } else if (person.getMemberSinceString() != null && !person.getMemberSinceString().trim().equals("")) {
                items.add(new ListEntry(person.getMemberSinceString(), null, R.string.person_subtitle_memberSince, R.drawable.ic_person_member_since));
            }

            // Add Events
            Map<String, Registration> events = person.getEvents();
            events.entrySet().forEach(event -> items.add(new ListEntry(
                    event.getKey(),
                    null,
                    event.getValue().getStatus().toStringRes(),
                    R.drawable.ic_person_event
            )));

            ListAdapter listAdapter = new ListAdapter(list.getContext(), items, view.findViewById(R.id.fixed_header));
            list.setAdapter(listAdapter);
            list.setOnScrollListener(listAdapter);
        }

        private static class ListEntry {
            private final String title;
            private final String subtitle;
            private final int subtitleResId;
            private final int icon;

            ListEntry(String title, String subtitle, int subtitleResId, int icon) {
                this.title = title;
                this.subtitle = subtitle;
                this.subtitleResId = subtitleResId;
                this.icon = icon;
            }
        }

        private static class ListAdapter extends FixedHeaderAdapter<ListEntry, Integer> {
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
