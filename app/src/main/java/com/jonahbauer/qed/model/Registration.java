package com.jonahbauer.qed.model;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.util.Pair;

import com.jonahbauer.qed.R;

import java.time.Instant;
import java.time.LocalDate;

import com.jonahbauer.qed.model.parcel.ParcelExtensions;
import it.unimi.dsi.fastutil.Hash;
import lombok.Data;
import lombok.ToString;

@Data
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
    private LocalDate personBirthday;
    private String personBirthdayString;
    private String personGender;
    private String personMail;
    private String personAddress;
    private String personPhone;

    // Transport
    private Instant timeOfArrival;
    private String timeOfArrivalString;
    private Instant timeOfDeparture;
    private String timeOfDepartureString;
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
    private LocalDate paymentTime;
    private String paymentTimeString;
    private Boolean memberAbatement;
    private String otherAbatement;

    private Instant loaded;

    public void setPerson(Person person) {
        this.person = person;
        personId = person.getId();
        personName = person.getFullName();
        personBirthday = person.getBirthday();
        personBirthdayString = person.getBirthdayString();
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
            person.setBirthdayString(personBirthdayString);
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
        return (talks != null && !talks.trim().equals(""))
                || (food != null && !food.trim().equals(""))
                || (notes != null && !notes.trim().equals(""));
    }

    public boolean hasTransportInformation() {
        return (timeOfArrivalString != null && !timeOfArrivalString.trim().equals(""))
                || (timeOfDepartureString != null && !timeOfDepartureString.trim().equals(""))
                || (overnightStays != null);
    }

    public boolean hasPaymentInformation() {
        return (paymentAmount != null)
                || (paymentDone != null)
                || (paymentTimeString != null && !paymentTimeString.trim().equals(""))
                || (memberAbatement != null)
                || (otherAbatement != null && !otherAbatement.trim().equals(""));
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
        ParcelExtensions.writeLocalDate(dest, personBirthday);
        dest.writeString(personBirthdayString);
        dest.writeString(personGender);
        dest.writeString(personMail);
        dest.writeString(personAddress);
        dest.writeString(personPhone);

        ParcelExtensions.writeInstant(dest, timeOfArrival);
        dest.writeString(timeOfArrivalString);
        ParcelExtensions.writeInstant(dest, timeOfDeparture);
        dest.writeString(timeOfDepartureString);
        dest.writeString(sourceStation);
        dest.writeString(targetStation);
        dest.writeString(railcard);
        dest.writeValue(overnightStays);

        dest.writeString(food);
        dest.writeString(talks);
        dest.writeString(notes);

        dest.writeValue(paymentAmount);
        dest.writeValue(paymentDone);
        ParcelExtensions.writeLocalDate(dest, paymentTime);
        dest.writeString(paymentTimeString);
        dest.writeValue(memberAbatement);
        dest.writeString(otherAbatement);

        ParcelExtensions.writeInstant(dest, loaded);
    }

    public static final Creator<Registration> CREATOR = new Creator<>() {
        @Override
        @SuppressLint("ParcelClassLoader")
        public Registration createFromParcel(Parcel in) {
            Registration registration = new Registration(in.readLong());

            registration.status = ParcelExtensions.readEnum(in, Status.values());
            registration.organizer = (Boolean) in.readValue(null);

            registration.eventId = in.readLong();
            registration.eventTitle = in.readString();

            registration.personId = in.readLong();
            registration.personName = in.readString();
            registration.personBirthday = ParcelExtensions.readLocalDate(in);
            registration.personBirthdayString = in.readString();
            registration.personGender = in.readString();
            registration.personMail = in.readString();
            registration.personAddress = in.readString();
            registration.personPhone = in.readString();

            registration.timeOfArrival = ParcelExtensions.readInstant(in);
            registration.timeOfArrivalString = in.readString();
            registration.timeOfDeparture = ParcelExtensions.readInstant(in);
            registration.timeOfDepartureString = in.readString();
            registration.sourceStation = in.readString();
            registration.targetStation = in.readString();
            registration.railcard = in.readString();
            registration.overnightStays = (Integer) in.readValue(null);

            registration.food = in.readString();
            registration.talks = in.readString();
            registration.notes = in.readString();

            registration.paymentAmount = (Double) in.readValue(null);
            registration.paymentDone = (Boolean) in.readValue(null);
            registration.paymentTime = ParcelExtensions.readLocalDate(in);
            registration.paymentTimeString = in.readString();
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

    public enum Status {
        PARTICIPATED, CONFIRMED, OPEN, CANCELLED;

        @StringRes
        public int toStringRes() {
            switch (this) {
                case PARTICIPATED:
                    return R.string.registration_status_participated;
                case CONFIRMED:
                    return R.string.registration_status_confirmed;
                case OPEN:
                    return R.string.registration_status_open;
                case CANCELLED:
                    return R.string.registration_status_cancelled;
                default:
                    return -1;
            }
        }

        @DrawableRes
        public int toDrawableRes() {
            switch (this) {
                case OPEN:
                    return R.drawable.ic_event_member_open;
                default:
                case CONFIRMED:
                case PARTICIPATED:
                    return R.drawable.ic_event_member_confirmed;
                case CANCELLED:
                    return R.drawable.ic_event_member_opt_out;
            }
        }

        @DrawableRes
        public int toDrawableResVariant() {
            switch (this) {
                case OPEN:
                    return R.drawable.ic_registration_status_open;
                default:
                case CONFIRMED:
                case PARTICIPATED:
                    return R.drawable.ic_registration_status_confirmed;
                case CANCELLED:
                    return R.drawable.ic_registration_status_cancelled;
            }
        }
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
