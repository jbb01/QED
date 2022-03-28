package com.jonahbauer.qed.model;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.jonahbauer.qed.model.parcel.ParcelExtensions;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class Person implements Parcelable {
    public static final long NO_ID = Long.MIN_VALUE;
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
    private transient String mComparableFirstName;
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private transient String mComparableLastName;

    private final long id;
    private String username;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;

    private String gender;

    private String birthdayString;
    private LocalDate birthday;

    private String homeStation;
    private String railcard;
    private String food;
    private String notes;

    private Boolean member;
    private Boolean active;

    private String dateOfJoiningString;
    private LocalDate dateOfJoining;

    private String leavingDateString;
    private LocalDate leavingDate;

    private Instant loaded;

    // Pair of type and number/name
    private final Set<Pair<String, String>> contacts = new LinkedHashSet<>();
    private final Set<String> addresses = new LinkedHashSet<>();
    private final Set<Registration> events = new ObjectLinkedOpenCustomHashSet<>(Registration.STRATEGY_ID);

    public String getFullName() {
        if (fullName != null && (firstName == null || lastName == null)) {
            return fullName;
        } else {
            return getFullName(false);
        }
    }

    public String getFullName(boolean inverted) {
        if (firstName == null) {
            return lastName;
        } else if (lastName == null) {
            return firstName;
        } else if (inverted) {
            return lastName + ", " + firstName;
        } else {
            return firstName + " " + lastName;
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
        if (gender != null) entries.add("\"gender\":\"" + gender + "\"");
        if (email != null) entries.add( "\"email\":\"" + email + "\"");
        if (homeStation != null) entries.add( "\"homeStation\":\"" + homeStation + "\"");
        if (railcard != null) entries.add( "\"railcard\":\"" + railcard + "\"");
        if (id != NO_ID) entries.add( "\"id\":" + id);
        if (member != null) entries.add( "\"member\":" + member);
        if (active != null) entries.add( "\"active\":" + active);
        if (dateOfJoining != null) entries.add( "\"memberSince\":\"" + dateOfJoining + "\"");
        if (!contacts.isEmpty()) entries.add( "\"contacts\":" + contacts.stream().map(number -> "{\"note\":\"" + number.first + "\", \"number\":\"" + number.second + "\"}").collect(Collectors.joining(", ", "[", "]")));
        if (!addresses.isEmpty()) entries.add( "\"addresses\":" + addresses);
        if (!events.isEmpty()) entries.add( "\"events\":" + events.stream().map(String::valueOf).collect(Collectors.joining(", ", "[", "]")));
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
        dest.writeString(fullName);
        dest.writeString(email);

        dest.writeString(gender);

        dest.writeString(birthdayString);
        ParcelExtensions.writeLocalDate(dest, birthday);

        dest.writeString(homeStation);
        dest.writeString(railcard);
        dest.writeString(food);
        dest.writeString(notes);

        dest.writeValue(member);
        dest.writeValue(active);

        dest.writeString(dateOfJoiningString);
        ParcelExtensions.writeLocalDate(dest, dateOfJoining);

        dest.writeString(leavingDateString);
        ParcelExtensions.writeLocalDate(dest, leavingDate);

        ParcelExtensions.writeInstant(dest, loaded);

        dest.writeInt(contacts.size());
        for (Pair<String, String> number : contacts) {
            dest.writeString(number.first);
            dest.writeString(number.second);
        }

        ParcelExtensions.writeStringCollection(dest, addresses);
        ParcelExtensions.writeTypedCollection(dest, events);
    }

    public static final Parcelable.Creator<Person> CREATOR = new Parcelable.Creator<>() {
        @NonNull
        @Override
        @SuppressLint("ParcelClassLoader")
        public Person createFromParcel(@NonNull Parcel source) {
            Person person = new Person(source.readLong());
            person.username = source.readString();
            person.firstName = source.readString();
            person.lastName = source.readString();
            person.fullName = source.readString();
            person.email = source.readString();

            person.gender = source.readString();

            person.birthdayString = source.readString();
            person.birthday = ParcelExtensions.readLocalDate(source);

            person.homeStation = source.readString();
            person.railcard = source.readString();
            person.food = source.readString();
            person.notes = source.readString();

            person.member = (Boolean) source.readValue(null);
            person.active = (Boolean) source.readValue(null);

            person.dateOfJoiningString = source.readString();
            person.dateOfJoining = ParcelExtensions.readLocalDate(source);

            person.leavingDateString = source.readString();
            person.leavingDate = ParcelExtensions.readLocalDate(source);

            person.loaded = ParcelExtensions.readInstant(source);

            int contactCount = source.readInt();
            for (int i = 0; i < contactCount; i++) {
                person.contacts.add(new Pair<>(
                        source.readString(),
                        source.readString()
                ));
            }

            ParcelExtensions.readStringCollection(source, person.addresses);
            ParcelExtensions.readTypedCollection(source, person.events, Registration.CREATOR);

            return person;
        }

        @Override
        public Person[] newArray(int size) {
            return new Person[size];
        }
    };
}
