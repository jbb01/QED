package com.jonahbauer.qed.model;

import android.annotation.SuppressLint;
import android.icu.text.Collator;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.util.Pair;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.model.parcel.ParcelExtensions;
import com.jonahbauer.qed.model.parcel.LambdaCreator;
import com.jonahbauer.qed.model.parcel.ParcelableEnum;
import com.jonahbauer.qed.model.util.ParsedLocalDate;
import com.jonahbauer.qed.util.TextUtils;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

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

    private Gender gender;
    private ParsedLocalDate birthday;

    private String homeStation;
    private String railcard;
    private String food;
    private String notes;

    private Boolean member;
    private Boolean active;
    private ParsedLocalDate dateOfJoining;
    private ParsedLocalDate dateOfQuitting;
    private ParsedLocalDate memberUntil;
    private ParsedLocalDate paidUntil;

    private Instant loaded;

    // Pair of type and number/name
    private final Set<Pair<String, String>> contacts = new LinkedHashSet<>();
    private final Set<String> addresses = new LinkedHashSet<>();
    private final Set<Registration> events = new ObjectLinkedOpenCustomHashSet<>(Registration.STRATEGY_ID);
    private final Set<String> groups = new LinkedHashSet<>();
    private final Set<Payment> payments = new LinkedHashSet<>();
    private EnumSet<Privacy> privacy;

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
                || !TextUtils.isNullOrBlank(notes)
                || !groups.isEmpty()
                || privacy != null;
    }

    @NonNull
    @Override
    public String toString() {
        var joiner = new StringJoiner(", ", "{", "}");
        if (firstName != null) joiner.add( "\"first_name\":\"" + firstName + "\"");
        if (lastName != null) joiner.add( "\"last_name\":\"" + lastName + "\"");
        if (username != null) joiner.add("\"username\":\"" + username + "\"");
        if (birthday != null) joiner.add( "\"birthday\":\"" + birthday + "\"");
        if (gender != null) joiner.add("\"gender\":\"" + gender + "\"");
        if (email != null) joiner.add( "\"email\":\"" + email + "\"");
        if (homeStation != null) joiner.add( "\"home_station\":\"" + homeStation + "\"");
        if (railcard != null) joiner.add( "\"railcard\":\"" + railcard + "\"");
        if (id != NO_ID) joiner.add( "\"id\":" + id);
        if (member != null) joiner.add( "\"member\":" + member);
        if (active != null) joiner.add( "\"active\":" + active);
        if (dateOfJoining != null) joiner.add( "\"member_since\":\"" + dateOfJoining + "\"");
        if (dateOfQuitting != null) joiner.add( "\"quit_at\":\"" + dateOfQuitting + "\"");
        if (memberUntil != null) joiner.add( "\"member_until\":\"" + memberUntil + "\"");
        if (paidUntil != null) joiner.add( "\"paid_until\":\"" + paidUntil + "\"");
        if (!contacts.isEmpty()) joiner.add( "\"contacts\":" + contacts.stream().map(number -> "{\"note\":\"" + number.first + "\", \"number\":\"" + number.second + "\"}").collect(Collectors.joining(", ", "[", "]")));
        if (!addresses.isEmpty()) joiner.add( "\"addresses\":" + addresses);
        if (!events.isEmpty()) joiner.add( "\"events\":" + events.stream().map(String::valueOf).collect(Collectors.joining(", ", "[", "]")));
        if (!groups.isEmpty()) joiner.add("\"groups\":" + groups);
        if (!payments.isEmpty()) joiner.add("\"payments\":" + payments);
        if (privacy != null) joiner.add("\"privacy\":" + privacy);
        return joiner.toString();
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

        dest.writeTypedObject(gender, flags);
        dest.writeTypedObject(birthday, flags);

        dest.writeString(homeStation);
        dest.writeString(railcard);
        dest.writeString(food);
        dest.writeString(notes);

        dest.writeValue(member);
        dest.writeValue(active);
        dest.writeTypedObject(dateOfJoining, flags);
        dest.writeTypedObject(dateOfQuitting, flags);
        dest.writeTypedObject(memberUntil, flags);
        dest.writeTypedObject(paidUntil, flags);

        ParcelExtensions.writeInstant(dest, loaded);

        dest.writeInt(contacts.size());
        for (Pair<String, String> number : contacts) {
            dest.writeString(number.first);
            dest.writeString(number.second);
        }

        ParcelExtensions.writeStringCollection(dest, addresses);
        ParcelExtensions.writeTypedCollection(dest, events);
        ParcelExtensions.writeStringCollection(dest, groups);
        ParcelExtensions.writeTypedCollection(dest, payments);
        ParcelExtensions.writeEnumSet(dest, privacy);
    }

    @SuppressLint("ParcelClassLoader")
    public static final Creator<Person> CREATOR = new LambdaCreator<>(Person[]::new, source -> {
        Person person = new Person(source.readLong());
        person.username = source.readString();
        person.firstName = source.readString();
        person.lastName = source.readString();
        person.fullName = source.readString();
        person.email = source.readString();

        person.gender = source.readTypedObject(Gender.CREATOR);
        person.birthday = source.readTypedObject(ParsedLocalDate.CREATOR);

        person.homeStation = source.readString();
        person.railcard = source.readString();
        person.food = source.readString();
        person.notes = source.readString();

        person.member = (Boolean) source.readValue(null);
        person.active = (Boolean) source.readValue(null);
        person.dateOfJoining = source.readTypedObject(ParsedLocalDate.CREATOR);
        person.dateOfQuitting = source.readTypedObject(ParsedLocalDate.CREATOR);
        person.memberUntil = source.readTypedObject(ParsedLocalDate.CREATOR);
        person.paidUntil = source.readTypedObject(ParsedLocalDate.CREATOR);

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
        ParcelExtensions.readStringCollection(source, person.groups);
        ParcelExtensions.readTypedCollection(source, person.payments, Payment.CREATOR);
        person.privacy = ParcelExtensions.readEnumSet(source, Privacy.CREATOR);

        return person;
    });

    @Data
    public static class Payment implements Parcelable {
        private ParsedLocalDate start;
        private ParsedLocalDate end;
        private String comment;
        private Type type;
        private Double amount;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeTypedObject(start, flags);
            dest.writeTypedObject(end, flags);
            dest.writeString(comment);
            dest.writeTypedObject(type, flags);
            dest.writeValue(amount);
        }

        @SuppressLint("ParcelClassLoader")
        public static final Creator<Payment> CREATOR = new LambdaCreator<>(Payment[]::new, source -> {
            var payment = new Payment();
            payment.setStart(source.readTypedObject(ParsedLocalDate.CREATOR));
            payment.setEnd(source.readTypedObject(ParsedLocalDate.CREATOR));
            payment.setComment(source.readString());
            payment.setType(source.readTypedObject(Type.CREATOR));
            payment.setAmount((Double) source.readValue(null));
            return payment;
        });

        @Getter
        @RequiredArgsConstructor
        public enum Type implements ParcelableEnum {
            REGULAR_MEMBER(R.string.person_payment_regular_member),
            SPONSOR_MEMBER(R.string.person_payment_sponsor_member),
            DONATION(R.string.person_payment_donation),
            OTHER(R.string.person_payment_other),
            FREE_MEMBER(R.string.person_payment_free_member),
            SPONSOR_AND_MEMBER(R.string.person_payment_sponsor_and_member);

            public static final Parcelable.Creator<Type> CREATOR = new ParcelableEnum.Creator<>(Type.values(), Type[]::new);

            private final @StringRes int stringRes;
        }
    }

    @Getter
    @RequiredArgsConstructor
    public enum Gender implements ParcelableEnum {
        MALE(R.string.person_gender_male, R.drawable.ic_person_gender_male),
        FEMALE(R.string.person_gender_female, R.drawable.ic_person_gender_female),
        OTHER(R.string.person_gender_other, R.drawable.ic_person_gender_other);
        public static final Parcelable.Creator<Gender> CREATOR = new ParcelableEnum.Creator<>(Gender.values(), Gender[]::new);

        private final @StringRes int stringRes;
        private final @DrawableRes int drawableRes;
    }

    @Getter
    @RequiredArgsConstructor
    public enum Privacy implements ParcelableEnum {
        NEWSLETTER(R.string.person_privacy_newsletter),
        PHOTOS(R.string.person_privacy_photos),
        PUBLIC_BIRTHDAY(R.string.person_privacy_public_birthday),
        PUBLIC_EMAIL(R.string.person_privacy_public_email),
        PUBLIC_ADDRESS(R.string.person_privacy_public_address),
        PUBLIC_PROFILE(R.string.person_privacy_public_profile);
        public static final ParcelableEnum.Creator<Privacy> CREATOR = new ParcelableEnum.Creator<>(Privacy.values(), Privacy[]::new);

        private final @StringRes int stringRes;
    }
}
