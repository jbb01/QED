package com.jonahbauer.qed.model;

import android.annotation.SuppressLint;
import android.icu.text.Collator;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import com.jonahbauer.qed.model.parcel.ParcelExtensions;
import com.jonahbauer.qed.model.util.ParsedLocalDate;
import com.jonahbauer.qed.util.TextUtils;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = "id")
public class Person implements Parcelable {
    public static final long NO_ID = Long.MIN_VALUE;

    private static final Collator COLLATOR = Collator.getInstance(Locale.ROOT);
    static {
        COLLATOR.setStrength(Collator.PRIMARY);
        COLLATOR.freeze();
    }
    private static final Comparator<String> COMPARATOR = Comparator.nullsLast(COLLATOR::compare);
    public static final Comparator<Person> COMPARATOR_FIRST_NAME = Comparator.nullsLast((p1, p2) -> {
        var out = COMPARATOR.compare(p1.getFirstName(), p2.getFirstName());
        if (out == 0) out = COMPARATOR.compare(p1.getLastName(), p2.getLastName());
        return out;
    });
    public static final Comparator<Person> COMPARATOR_LAST_NAME = Comparator.nullsLast((p1, p2) -> {
        var out = COMPARATOR.compare(p1.getLastName(), p2.getLastName());
        if (out == 0) out = COMPARATOR.compare(p1.getFirstName(), p2.getFirstName());
        return out;
    });

    private final long id;
    private String username;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;

    private String gender;
    private ParsedLocalDate birthday;

    private String homeStation;
    private String railcard;
    private String food;
    private String notes;

    private Boolean member;
    private Boolean active;
    private ParsedLocalDate dateOfJoining;
    private ParsedLocalDate dateOfQuitting;

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
        return !TextUtils.isNullOrBlank(homeStation)
                || !TextUtils.isNullOrBlank(railcard)
                || !TextUtils.isNullOrBlank(food)
                || !TextUtils.isNullOrBlank(notes);
    }

    @NonNull
    @Override
    public String toString() {
        List<String> entries = new LinkedList<>();
        if (firstName != null) entries.add( "\"first_name\":\"" + firstName + "\"");
        if (lastName != null) entries.add( "\"last_name\":\"" + lastName + "\"");
        if (username != null) entries.add("\"username\":\"" + username + "\"");
        if (birthday != null) entries.add( "\"birthday\":\"" + birthday + "\"");
        if (gender != null) entries.add("\"gender\":\"" + gender + "\"");
        if (email != null) entries.add( "\"email\":\"" + email + "\"");
        if (homeStation != null) entries.add( "\"home_station\":\"" + homeStation + "\"");
        if (railcard != null) entries.add( "\"railcard\":\"" + railcard + "\"");
        if (id != NO_ID) entries.add( "\"id\":" + id);
        if (member != null) entries.add( "\"member\":" + member);
        if (active != null) entries.add( "\"active\":" + active);
        if (dateOfJoining != null) entries.add( "\"member_since\":\"" + dateOfJoining + "\"");
        if (dateOfQuitting != null) entries.add( "\"quit_at\":\"" + dateOfQuitting + "\"");
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
        dest.writeTypedObject(birthday, flags);

        dest.writeString(homeStation);
        dest.writeString(railcard);
        dest.writeString(food);
        dest.writeString(notes);

        dest.writeValue(member);
        dest.writeValue(active);
        dest.writeTypedObject(dateOfJoining, flags);
        dest.writeTypedObject(dateOfQuitting, flags);

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
            person.birthday = source.readTypedObject(ParsedLocalDate.CREATOR);

            person.homeStation = source.readString();
            person.railcard = source.readString();
            person.food = source.readString();
            person.notes = source.readString();

            person.member = (Boolean) source.readValue(null);
            person.active = (Boolean) source.readValue(null);
            person.dateOfJoining = source.readTypedObject(ParsedLocalDate.CREATOR);
            person.dateOfQuitting = source.readTypedObject(ParsedLocalDate.CREATOR);

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
