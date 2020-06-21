package com.jonahbauer.qed.qeddb.person;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.jonahbauer.qed.qeddb.event.Event;
import com.jonahbauer.qed.util.Triple;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

public class Person implements Parcelable {
    public static final Comparator<Person> COMPARATOR_FIRST_NAME = Comparator.comparing(
            person -> {
                if (person.mComparableFirstName == null) person.mComparableFirstName = (person.firstName + " " + person.lastName).replaceAll("Ö", "O").replaceAll("Ü", "U").replaceAll("Ä","A");
                return person.mComparableFirstName;
            });
    public static final Comparator<Person> COMPARATOR_LAST_NAME = Comparator.comparing(
            person -> {
                if (person.mComparableLastName == null) person.mComparableLastName = (person.lastName + " " + person.firstName).replaceAll("Ö", "O").replaceAll("Ü", "U").replaceAll("Ä","A");
                return person.mComparableLastName;
            });

    private String mComparableFirstName;
    private String mComparableLastName;

    public long id;
    public String firstName;
    public String lastName;
    public String birthday;
    public String email;
    public String homeStation;
    public String railcard;
    public boolean member;
    public boolean active;
    public String memberSince;
    // Pair of type and number
    public ArrayList<Pair<String, String>> phoneNumbers;
    public ArrayList<String> addresses;
    // Pair of event and roll
    public ArrayList<Pair<Event,String>> events;
    // Pair of (roll and pair of (start and end))
    public ArrayList<Triple<String, String, String>> management;
    public ParticipationStatus participationStatus;

    public Person() {
        phoneNumbers = new ArrayList<>();
        addresses = new ArrayList<>();
        events = new ArrayList<>();
        management = new ArrayList<>();
    }

    @NonNull
    @Override
    public String toString() {
        return "{" +
                "\"firstName\":\"" + firstName + "\"," +
                "\"lastName\":\"" + lastName + "\"," +
                "\"birthday\":\"" + birthday + "\"," +
                "\"email\":\"" + email + "\"," +
                "\"homeStation\":\"" + homeStation + "\"," +
                "\"railcard\":\"" + railcard + "\"," +
                "\"id\":" + id + "," +
                "\"member\":" + member + "," +
                "\"active\":" + active + "," +
                "\"memberSince\":\"" + memberSince + "\"," +
                "\"phoneNumbers\":" + phoneNumbers.stream().map(number -> "{\"note\":\"" + number.first + "\", \"number\":\"" + number.second + "\"}").collect(Collectors.joining(", ", "[", "]")) + "," +
                "\"addresses\":" + addresses + "," +
                "\"events\":" + events.stream().map(event -> "{\"event\":\"" + event.first + "\", \"role\":\"" + event.second + "\"}").collect(Collectors.joining(", ", "[", "]")) + "," +
                "\"management\":" + management.stream().map(mgmnt -> "{\"role\":\"" + mgmnt.first + "\", \"start\":\"" + mgmnt.second + "\", \"end\":\"" + mgmnt.third + "\"}").collect(Collectors.joining(", ", "[", "]")) + "," +
                "\"type\":\"" + participationStatus + "\"" +
                "}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(firstName);
        dest.writeString(lastName);
        dest.writeString(birthday);
        dest.writeString(email);
        dest.writeString(homeStation);
        dest.writeString(railcard);
        dest.writeLong(id);
        dest.writeInt((member ? 0 : 2) + (active ? 0 : 1));
        dest.writeString(memberSince);
        dest.writeInt(phoneNumbers.size());
        for (Pair<String, String> number : phoneNumbers) {
            dest.writeString(number.first);
            dest.writeString(number.second);
        }
        dest.writeStringList(addresses);
        dest.writeInt(events.size());
        for (Pair<Event, String> event : events) {
            dest.writeParcelable(event.first, flags);
            dest.writeString(event.second);
        }
        dest.writeInt(management.size());
        for (Triple<String, String,String> mgmnt : management) {
            dest.writeString(mgmnt.first);
            dest.writeString(mgmnt.second);
            dest.writeString(mgmnt.third);
        }
        dest.writeInt(participationStatus != null ? participationStatus.ordinal() : -1);
    }

    public static final Parcelable.Creator<Person> CREATOR = new Parcelable.Creator<Person>() {
        @NonNull
        @Override
        public Person createFromParcel(@NonNull Parcel source) {
            Person person = new Person();
            person.firstName = source.readString();
            person.lastName = source.readString();
            person.birthday = source.readString();
            person.email = source.readString();
            person.homeStation = source.readString();
            person.railcard = source.readString();
            person.id = source.readLong();
            int memberActive = source.readInt();
            person.member = memberActive / 2 == 1;
            person.active = memberActive % 2 == 1;

            int phoneNumberCount = source.readInt();
            for (int i = 0; i < phoneNumberCount; i++) {
                person.phoneNumbers.add(new Pair<>(
                        source.readString(),
                        source.readString()
                ));
            }

            source.readStringList(person.addresses);

            int eventCount = source.readInt();
            for (int i = 0; i < eventCount; i++) {
                person.events.add(new Pair<>(
                        source.readParcelable(ClassLoader.getSystemClassLoader()),
                        source.readString()
                ));
            }

            int mgmntCount = source.readInt();
            for (int i = 0; i < mgmntCount; i++) {
                person.management.add(new Triple<>(
                        source.readString(),
                        source.readString(),
                        source.readString()
                ));
            }

            int memberType = source.readInt();
            if (memberType != -1)
                person.participationStatus = ParticipationStatus.values()[memberType];
            return person;
        }

        @Override
        public Person[] newArray(int size) {
            return new Person[size];
        }
    };

    public enum ParticipationStatus {
        ORGA, MEMBER_PARTICIPATED, MEMBER_CONFIRMED, MEMBER_OPEN, MEMBER_OPT_OUT;

        @Override
        public String toString() {
            switch (this) {
                case ORGA:
                    return "Orga";
                case MEMBER_PARTICIPATED:
                    return "teilgenommen";
                case MEMBER_CONFIRMED:
                    return "bestätigt";
                case MEMBER_OPEN:
                    return "offen";
                case MEMBER_OPT_OUT:
                    return "abgemeldet";
            }
            return "null";
        }
    }
}
