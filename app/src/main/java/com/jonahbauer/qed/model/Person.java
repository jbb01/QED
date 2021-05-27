package com.jonahbauer.qed.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Data;

@Data
public class Person implements Parcelable {
    public static final Comparator<Person> COMPARATOR_FIRST_NAME = Comparator.nullsLast(Comparator.comparing(
            person -> {
                if (person.mComparableFirstName == null) person.mComparableFirstName = (person.firstName + " " + person.lastName).replaceAll("Ö", "O").replaceAll("Ü", "U").replaceAll("Ä","A");
                return person.mComparableFirstName;
            }));
    public static final Comparator<Person> COMPARATOR_LAST_NAME = Comparator.nullsLast(Comparator.comparing(
            person -> {
                if (person.mComparableLastName == null) person.mComparableLastName = (person.lastName + " " + person.firstName).replaceAll("Ö", "O").replaceAll("Ü", "U").replaceAll("Ä","A");
                return person.mComparableLastName;
            }));

    private String mComparableFirstName;
    private String mComparableLastName;

    private final long id;
    private String firstName;
    private String lastName;
    private String email;

    private String birthdayString;
    private Date birthday;

    private String homeStation;
    private String railcard;

    private boolean member;
    private boolean active;

    private String memberSinceString;
    private Date memberSince;

    // Pair of type and number/name
    private ArrayList<Pair<String, String>> contacts = new ArrayList<>();
    private ArrayList<String> addresses = new ArrayList<>();
    private Map<String, Registration> events = new LinkedHashMap<>();

    @NonNull
    @Override
    public String toString() {
        List<String> entries = new LinkedList<>();
        if (firstName != null) entries.add( "\"firstName\":\"" + firstName + "\"");
        if (lastName != null) entries.add( "\"lastName\":\"" + lastName + "\"");
        if (birthdayString != null) entries.add( "\"birthday\":\"" + birthdayString + "\"");
        if (email != null) entries.add( "\"email\":\"" + email + "\"");
        if (homeStation != null) entries.add( "\"homeStation\":\"" + homeStation + "\"");
        if (railcard != null) entries.add( "\"railcard\":\"" + railcard + "\"");
        if (id != -1) entries.add( "\"id\":" + id);
        entries.add( "\"member\":" + member);
        entries.add( "\"active\":" + active);
        if (memberSince != null) entries.add( "\"memberSince\":\"" + memberSince + "\"");
        if (!contacts.isEmpty()) entries.add( "\"contacts\":" + contacts.stream().map(number -> "{\"note\":\"" + number.first + "\", \"number\":\"" + number.second + "\"}").collect(Collectors.joining(", ", "[", "]")));
        if (!addresses.isEmpty()) entries.add( "\"addresses\":" + addresses);
        if (!events.isEmpty()) entries.add( "\"events\":" + events.entrySet().stream().map(event -> "{\"event\":\"" + event.getKey() + "\", \"registration\":\"" + event.getValue() + "\"}").collect(Collectors.joining(", ", "[", "]")));
        return entries.stream().collect(Collectors.joining(", ", "{", "}"));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(firstName);
        dest.writeString(lastName);
        dest.writeString(birthdayString);
        dest.writeSerializable(birthday);
        dest.writeString(email);
        dest.writeString(homeStation);
        dest.writeString(railcard);
        dest.writeInt((member ? 2 : 0) + (active ? 1 : 0));
        dest.writeString(memberSinceString);
        dest.writeSerializable(memberSince);
        dest.writeInt(contacts.size());
        for (Pair<String, String> number : contacts) {
            dest.writeString(number.first);
            dest.writeString(number.second);
        }
        dest.writeStringList(addresses);
        dest.writeInt(events.size());
        for (Map.Entry<String, Registration> event : events.entrySet()) {
            dest.writeString(event.getKey());
            dest.writeLong(event.getValue().getId());
        }
    }

    public static final Parcelable.Creator<Person> CREATOR = new Parcelable.Creator<Person>() {
        @NonNull
        @Override
        public Person createFromParcel(@NonNull Parcel source) {
            Person person = new Person(source.readLong());
            person.firstName = source.readString();
            person.lastName = source.readString();
            person.birthdayString = source.readString();
            person.birthday = (Date) source.readSerializable();
            person.email = source.readString();
            person.homeStation = source.readString();
            person.railcard = source.readString();
            int memberActive = source.readInt();
            person.member = memberActive / 2 == 1;
            person.active = memberActive % 2 == 1;
            person.memberSinceString = source.readString();
            person.memberSince = (Date) source.readSerializable();

            int phoneNumberCount = source.readInt();
            for (int i = 0; i < phoneNumberCount; i++) {
                person.contacts.add(new Pair<>(
                        source.readString(),
                        source.readString()
                ));
            }

            source.readStringList(person.addresses);

            int eventCount = source.readInt();
            for (int i = 0; i < eventCount; i++) {
                person.events.put(
                        source.readString(),
                        new Registration(source.readLong())
                );
            }

            return person;
        }

        @Override
        public Person[] newArray(int size) {
            return new Person[size];
        }
    };
}