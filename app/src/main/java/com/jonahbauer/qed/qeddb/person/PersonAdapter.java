package com.jonahbauer.qed.qeddb.person;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.layoutStuff.FixedHeaderAdapter;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class PersonAdapter extends FixedHeaderAdapter<Person, Character> {
    public static final String SORT_FIRST_NAME = "firstName";
    public static final String SORT_LAST_NAME = "lastName";

    private String sort;

    private static Comparator<Person> comparatorFirstName = Comparator.comparing(
            person -> {
                if (person.comparableFirstName == null) person.comparableFirstName = (person.firstName + " " + person.lastName).replaceAll("Ö", "O").replaceAll("Ü", "U").replaceAll("Ä","A");
                return person.comparableFirstName;
            });
    private static Comparator<Person> comparatorLastName = Comparator.comparing(
            person -> {
                if (person.comparableLastName == null) person.comparableLastName = (person.lastName + " " + person.firstName).replaceAll("Ö", "O").replaceAll("Ü", "U").replaceAll("Ä","A");
                return person.comparableLastName;
            });
    private static Function<Person, Character> headerMapFirstName = person -> person.firstName.toUpperCase().toUpperCase().replaceAll("Ö", "O").replaceAll("Ü", "U").replaceAll("Ä","A").charAt(0);
    private static Function<Person, Character> headerMapLastName = person -> person.lastName.toUpperCase().toUpperCase().replaceAll("Ö", "O").replaceAll("Ü", "U").replaceAll("Ä","A").charAt(0);

    public PersonAdapter(Context context, @NonNull List<Person> itemList, String sort, View fixedHeader) {
        super(context, itemList, headerMapFirstName, comparatorFirstName, fixedHeader);
        setSort(sort);
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
        switch ((person.firstName + person.lastName).chars().sum()%10) {
            case 0:
                initialsCircle.setColorFilter(Color.argb(255, 0x33, 0xb5, 0xe5));
                break;
            case 1:
                initialsCircle.setColorFilter(Color.argb(255, 0x99, 0xcc, 0x00));
                break;
            case 2:
                initialsCircle.setColorFilter(Color.argb(255, 0xff, 0x44, 0x44));
                break;
            case 3:
                initialsCircle.setColorFilter(Color.argb(255, 0x00, 0x99, 0xcc));
                break;
            case 4:
                initialsCircle.setColorFilter(Color.argb(255, 0x66, 0x99, 0x00));
                break;
            case 5:
                initialsCircle.setColorFilter(Color.argb(255, 0xcc, 0x00, 0x00));
                break;
            case 6:
                initialsCircle.setColorFilter(Color.argb(255, 0xaa, 0x66, 0xcc));
                break;
            case 7:
                initialsCircle.setColorFilter(Color.argb(255, 0xff, 0xbb, 0x33));
                break;
            case 8:
                initialsCircle.setColorFilter(Color.argb(255, 0xff, 0x88, 0x00));
                break;
            case 9:
                initialsCircle.setColorFilter(Color.argb(255, 0x00, 0xdd, 0xff));
                break;
        }


        String name = "";
        if (sort.equals(SORT_FIRST_NAME)) name = person.firstName + " " + person.lastName;
        else if (sort.equals(SORT_LAST_NAME)) name = person.lastName + ", " + person.firstName;
        String email = person.email;
        String active = !person.active ? "(inaktiv)" : (!person.member ? "(kein Mitglied)" : "");
        ((TextView)view.findViewById(R.id.person_name)).setText(name);
        ((TextView)view.findViewById(R.id.person_email)).setText(email);
        ((TextView)view.findViewById(R.id.person_active)).setText(active);
        ((TextView)view.findViewById(R.id.header)).setText("");

        if (sort.equals(SORT_FIRST_NAME)) ((TextView)view.findViewById(R.id.person_initials)).setText(String.format("%s%s", String.valueOf(person.firstName.charAt(0)), String.valueOf(person.lastName).charAt(0)));
        else if (sort.equals(SORT_LAST_NAME)) ((TextView)view.findViewById(R.id.person_initials)).setText(String.format("%s%s", String.valueOf(person.lastName.charAt(0)), String.valueOf(person.firstName).charAt(0)));

        return view;
    }

    @Override
    protected void setHeader(@NonNull View view, Character header) {
        ((TextView)view.findViewById(R.id.header)).setText(String.valueOf(header));
    }

    public void setSort(String sort) {
        if (sort.equals(SORT_LAST_NAME)) {
            setHeaderMap(headerMapLastName);
            setComparator(comparatorLastName);
            this.sort = SORT_LAST_NAME;
        } else {
            setHeaderMap(headerMapFirstName);
            setComparator(comparatorFirstName);
            this.sort = SORT_FIRST_NAME;
        }
    }
}
