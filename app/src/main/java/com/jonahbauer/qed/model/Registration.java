package com.jonahbauer.qed.model;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.util.Pair;

import com.jonahbauer.qed.R;

import java.time.Instant;

import com.jonahbauer.qed.model.parcel.ParcelExtensions;
import com.jonahbauer.qed.model.util.ParsedLocalDate;
import com.jonahbauer.qed.model.util.ParsedInstant;
import com.jonahbauer.qed.util.TextUtils;
import it.unimi.dsi.fastutil.Hash;
import lombok.*;

@Data
@EqualsAndHashCode(of = "id")
public class Registration implements Parcelable {
    public static final long NO_ID = Long.MIN_VALUE;
    private final long id;

    private Status status;
    private Boolean organizer;

    // cached values to be used when navigating
    @ToString.Exclude
    private transient Person person;
    @ToString.Exclude
    private transient Event event;

    // Event
    private long eventId = Event.NO_ID;
    private String eventTitle;

    // Person
    private long personId = Person.NO_ID;
    private String personName;
    private ParsedLocalDate personBirthday;
    private String personGender;
    private String personMail;
    private String personAddress;
    private String personPhone;

    // Transport
    private ParsedInstant timeOfArrival;
    private ParsedInstant timeOfDeparture;
    private String sourceStation;
    private String targetStation;
    private String railcard;
    private Integer overnightStays;

    // Additional
    private String food;
    private String talks;
    private String notes;

    // Payment
    private Double paymentAmount;
    private Boolean paymentDone;
    private ParsedLocalDate paymentTime;
    private Boolean memberAbatement;
    private String otherAbatement;

    private Instant loaded;

    public void setPerson(Person person) {
        this.person = person;
        personId = person.getId();
        personName = person.getFullName();
        personBirthday = person.getBirthday();
        personGender = person.getGender();
        personMail = person.getEmail();

        var addresses = person.getAddresses().iterator();
        if (addresses.hasNext()) {
            personAddress = addresses.next();
        } else {
            personAddress = null;
        }

        for (Pair<String, String> contact : person.getContacts()) {
            if (contact.first.equalsIgnoreCase("mobil")) {
                personPhone = contact.second;
                break;
            }
        }
    }

    public Person getPerson() {
        if (person == null && personId != Person.NO_ID) {
            person = new Person(personId);
            person.setFullName(personName);
            person.setBirthday(personBirthday);
            person.setGender(personGender);
            person.setEmail(personMail);
            if (personAddress != null) {
                person.getAddresses().add(personAddress);
            }
        }
        return person;
    }

    public void setEvent(Event event) {
        this.event = event;
        eventId = event.getId();
        eventTitle = event.getTitle();
    }

    public Event getEvent() {
        if (event == null && eventId != Event.NO_ID) {
            event = new Event(eventId);
            event.setTitle(eventTitle);
        }
        return event;
    }

    public boolean isOrganizer() {
        return getOrganizer() == Boolean.TRUE;
    }

    public boolean hasAdditionalInformation() {
        return !TextUtils.isNullOrBlank(talks)
                || !TextUtils.isNullOrBlank(food)
                || !TextUtils.isNullOrBlank(notes);
    }

    public boolean hasTransportInformation() {
        return timeOfArrival != null || timeOfDeparture != null || overnightStays != null
                || !TextUtils.isNullOrBlank(sourceStation) || !TextUtils.isNullOrBlank(targetStation)
                || !TextUtils.isNullOrBlank(railcard);
    }

    public boolean hasPaymentInformation() {
        return paymentAmount != null || paymentDone != null || paymentTime != null
                || memberAbatement != null || !TextUtils.isNullOrBlank(otherAbatement);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        ParcelExtensions.writeEnum(dest, status);
        dest.writeValue(organizer);

        dest.writeLong(eventId);
        dest.writeString(eventTitle);

        dest.writeLong(personId);
        dest.writeString(personName);
        dest.writeTypedObject(personBirthday, flags);
        dest.writeString(personGender);
        dest.writeString(personMail);
        dest.writeString(personAddress);
        dest.writeString(personPhone);

        dest.writeTypedObject(timeOfArrival, flags);
        dest.writeTypedObject(timeOfDeparture, flags);
        dest.writeString(sourceStation);
        dest.writeString(targetStation);
        dest.writeString(railcard);
        dest.writeValue(overnightStays);

        dest.writeString(food);
        dest.writeString(talks);
        dest.writeString(notes);

        dest.writeValue(paymentAmount);
        dest.writeValue(paymentDone);
        dest.writeTypedObject(paymentTime, flags);
        dest.writeValue(memberAbatement);
        dest.writeString(otherAbatement);

        ParcelExtensions.writeInstant(dest, loaded);
    }

    public static final Creator<Registration> CREATOR = new Creator<>() {
        @Override
        @SuppressLint("ParcelClassLoader")
        public Registration createFromParcel(Parcel in) {
            Registration registration = new Registration(in.readLong());

            registration.status = ParcelExtensions.readEnum(in, Status::get);
            registration.organizer = (Boolean) in.readValue(null);

            registration.eventId = in.readLong();
            registration.eventTitle = in.readString();

            registration.personId = in.readLong();
            registration.personName = in.readString();
            registration.personBirthday = in.readTypedObject(ParsedLocalDate.CREATOR);
            registration.personGender = in.readString();
            registration.personMail = in.readString();
            registration.personAddress = in.readString();
            registration.personPhone = in.readString();

            registration.timeOfArrival = in.readTypedObject(ParsedInstant.CREATOR);
            registration.timeOfDeparture = in.readTypedObject(ParsedInstant.CREATOR);
            registration.sourceStation = in.readString();
            registration.targetStation = in.readString();
            registration.railcard = in.readString();
            registration.overnightStays = (Integer) in.readValue(null);

            registration.food = in.readString();
            registration.talks = in.readString();
            registration.notes = in.readString();

            registration.paymentAmount = (Double) in.readValue(null);
            registration.paymentDone = (Boolean) in.readValue(null);
            registration.paymentTime = in.readTypedObject(ParsedLocalDate.CREATOR);
            registration.memberAbatement = (Boolean) in.readValue(null);
            registration.otherAbatement = in.readString();

            registration.loaded = ParcelExtensions.readInstant(in);

            return registration;
        }

        @Override
        public Registration[] newArray(int size) {
            return new Registration[size];
        }
    };

    @Getter
    @RequiredArgsConstructor
    public enum Status {
        PARTICIPATED(R.string.registration_status_participated, R.drawable.ic_event_member_confirmed, R.drawable.ic_registration_status_confirmed),
        CONFIRMED(R.string.registration_status_confirmed, R.drawable.ic_event_member_confirmed, R.drawable.ic_registration_status_confirmed),
        OPEN(R.string.registration_status_open, R.drawable.ic_event_member_open, R.drawable.ic_registration_status_open),
        CANCELLED(R.string.registration_status_cancelled, R.drawable.ic_event_member_opt_out, R.drawable.ic_registration_status_cancelled);

        private static final Status[] VALUES = Status.values();
        public static Status get(int ordinal) { return VALUES[ordinal]; }

        private final @StringRes int stringRes;
        private final @DrawableRes int drawableRes;
        private final @DrawableRes int drawableResVariant;
    }

    /**
     * A {@link Hash.Strategy} only comparing the {@linkplain Registration#getId() registration id}.
     */
    public static final Hash.Strategy<Registration> STRATEGY_ID = new Hash.Strategy<>() {
        @Override
        public int hashCode(Registration o) {
            return o == null ? 0 : Long.hashCode(o.getId());
        }

        @Override
        public boolean equals(Registration a, Registration b) {
            if (a == null ^ b == null) {
                return false;
            } else if (a == null) {
                return true;
            } else {
                return a.getId() == b.getId();
            }
        }
    };
}
