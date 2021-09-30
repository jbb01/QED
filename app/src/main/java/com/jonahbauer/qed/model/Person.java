package com.jonahbauer.qed.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class Person implements Parcelable {
    public static final Comparator<Person> COMPARATOR_FIRST_NAME = Comparator.nullsLast(Comparator.comparing(
            person -> {
                if (person.mComparableFirstName == null) {
                    person.mComparableFirstName = (person.firstName + " " + person.lastName)
                            .replace('Ö', 'O')
                            .replace('Ü', 'U')
                            .replace('Ä','A');
                }
                return person.mComparableFirstName;
            }));
    public static final Comparator<Person> COMPARATOR_LAST_NAME = Comparator.nullsLast(Comparator.comparing(
            person -> {
                if (person.mComparableLastName == null) {
                    person.mComparableLastName = (person.lastName + " " + person.firstName)
                            .replace('Ö', 'O')
                            .replace('Ü', 'U')
                            .replace('Ä','A');
                }
                return person.mComparableLastName;
            }));

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String mComparableFirstName;
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String mComparableLastName;

    private final long id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;

    private String birthdayString;
    private LocalDate birthday;

    private String homeStation;
    private String railcard;
    private String food;
    private String notes;

    private boolean member;
    private boolean active;

    private String dateOfJoiningString;
    private LocalDate dateOfJoining;

    private String leavingDateString;
    private LocalDate leavingDate;

    private boolean loaded;

    // Pair of type and number/name
    private ArrayList<Pair<String, String>> contacts = new ArrayList<>();
    private ArrayList<String> addresses = new ArrayList<>();
    private Map<String, Registration> events = new LinkedHashMap<>();

    public String getFullName() {
        return getFullName(false);
    }

    public String getFullName(boolean inverted) {
        if (inverted) {
            return getLastName() + ", " + getFirstName();
        } else {
            return getFirstName() + " " + getLastName();
        }
    }

    public String getInitials(boolean inverted) {
        String out = "";
        String first = inverted ? lastName : firstName;
        String last = inverted ? firstName : lastName;

        if (first != null && first.length() > 0) {
            out += first.charAt(0);
        }

        if (last != null && last.length() > 0) {
            out += last.charAt(0);
        }

        return out;
    }

    public boolean hasAdditionalInformation() {
        return (homeStation != null && !homeStation.trim().equals(""))
                || (railcard != null && !railcard.trim().equals(""))
                || (food != null && !food.trim().equals(""))
                || (notes != null && !notes.trim().equals(""));
    }

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
        if (dateOfJoining != null) entries.add( "\"memberSince\":\"" + dateOfJoining + "\"");
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
        dest.writeString(username);
        dest.writeString(firstName);
        dest.writeString(lastName);
        dest.writeString(birthdayString);
        dest.writeSerializable(birthday);
        dest.writeString(email);
        dest.writeString(homeStation);
        dest.writeString(railcard);
        dest.writeInt((member ? 2 : 0) + (active ? 1 : 0));
        dest.writeString(dateOfJoiningString);
        dest.writeSerializable(dateOfJoining);
        dest.writeString(leavingDateString);
        dest.writeSerializable(leavingDate);
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
        dest.writeString(food);
        dest.writeString(notes);
        dest.writeByte((byte)(loaded ? 1 : 0));
    }

    public static final Parcelable.Creator<Person> CREATOR = new Parcelable.Creator<>() {
        @NonNull
        @Override
        public Person createFromParcel(@NonNull Parcel source) {
            Person person = new Person(source.readLong());
            person.username = source.readString();
            person.firstName = source.readString();
            person.lastName = source.readString();
            person.birthdayString = source.readString();
            person.birthday = (LocalDate) source.readSerializable();
            person.email = source.readString();
            person.homeStation = source.readString();
            person.railcard = source.readString();
            int memberActive = source.readInt();
            person.member = memberActive / 2 == 1;
            person.active = memberActive % 2 == 1;
            person.dateOfJoiningString = source.readString();
            person.dateOfJoining = (LocalDate) source.readSerializable();
            person.leavingDateString = source.readString();
            person.leavingDate = (LocalDate) source.readSerializable();

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

            person.food = source.readString();
            person.notes = source.readString();

            person.loaded = source.readByte() != 0;

            return person;
        }

        @Override
        public Person[] newArray(int size) {
            return new Person[size];
        }
    };
}
