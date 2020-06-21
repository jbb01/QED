package com.jonahbauer.qed.qeddb.person;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.layoutStuff.FixedHeaderAdapter;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static com.jonahbauer.qed.qeddb.person.Person.COMPARATOR_FIRST_NAME;
import static com.jonahbauer.qed.qeddb.person.Person.COMPARATOR_LAST_NAME;

public class PersonAdapter extends FixedHeaderAdapter<Person, Character> {
    private static final Function<Person, Character> headerMapFirstName = person -> {
        if (person.firstName == null) return '?';
        else {
            char chr = person.firstName.charAt(0);

            switch (chr) {
                case 'Ä':
                case 'ä':
                    return 'A';
                case 'Ö':
                case 'ö':
                    return 'O';
                case 'Ü':
                case 'ü':
                    return 'U';
                default:
                    if ('a' <= chr && chr <= 'z') chr -= 0x32;
                    return chr;
            }
        }
    };
    private static final Function<Person, Character> headerMapLastName = person -> {
        if (person.lastName == null) return '?';
        else {
            char chr = person.lastName.charAt(0);

            switch (chr) {
                case 'Ä':
                case 'ä':
                    return 'A';
                case 'Ö':
                case 'ö':
                    return 'O';
                case 'Ü':
                case 'ü':
                    return 'U';
                default:
                    if ('a' <= chr && chr <= 'z') chr -= 0x32;
                    return chr;
            }
        }
    };

    private SortMode mSort;

    public PersonAdapter(Context context, @NonNull List<Person> itemList, SortMode sort, View fixedHeader) {
        super(context, itemList, headerMapFirstName, COMPARATOR_FIRST_NAME, fixedHeader);
        setSortMode(sort);
    }

    @NonNull
    @Override
    protected View getItemView(Person person, @Nullable View convertView, @NonNull ViewGroup parent, LayoutInflater inflater) {
        View view;
        if (convertView != null) {
            view = convertView;
        } else {
            view = Objects.requireNonNull(inflater).inflate(R.layout.list_item_person, parent, false);
        }

        ImageView initialsCircle = view.findViewById(R.id.person_initials_circle);
        initialsCircle.setColorFilter(Application.colorful((person.firstName + person.lastName).chars().sum()));

        String name = "";
        if (mSort == SortMode.FIRST_NAME) name = person.firstName + " " + person.lastName;
        else if (mSort == SortMode.LAST_NAME) name = person.lastName + ", " + person.firstName;
        String email = person.email;
        String active = !person.active ? "(inaktiv)" : (!person.member ? "(kein Mitglied)" : "");
        ((TextView)view.findViewById(R.id.person_name)).setText(name);
        ((TextView)view.findViewById(R.id.person_email)).setText(email);
        ((TextView)view.findViewById(R.id.person_active)).setText(active);
        ((TextView)view.findViewById(R.id.header)).setText("");

        if (mSort == SortMode.FIRST_NAME) ((TextView)view.findViewById(R.id.person_initials)).setText(String.format("%s%s", person.firstName.charAt(0), String.valueOf(person.lastName).charAt(0)));
        else if (mSort == SortMode.LAST_NAME) ((TextView)view.findViewById(R.id.person_initials)).setText(String.format("%s%s", person.lastName.charAt(0), String.valueOf(person.firstName).charAt(0)));

        return view;
    }

    @Override
    protected void setHeader(@NonNull View view, Character header) {
        ((TextView)view.findViewById(R.id.header)).setText(String.valueOf(header));
    }

    public void setSortMode(@NonNull SortMode sort) {
        switch (sort) {
            case FIRST_NAME:
                setHeaderMap(headerMapFirstName);
                setComparator(COMPARATOR_FIRST_NAME);
                break;
            case LAST_NAME:
                setHeaderMap(headerMapLastName);
                setComparator(COMPARATOR_LAST_NAME);
                break;
        }

        this.mSort = sort;
    }

    public enum SortMode {
        FIRST_NAME, LAST_NAME
    }
}
