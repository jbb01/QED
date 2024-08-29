package eu.jonahbauer.qed.model;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.util.Pair;

import eu.jonahbauer.qed.R;

import java.time.Instant;

import eu.jonahbauer.qed.model.contact.ContactDetailType;
import eu.jonahbauer.qed.model.parcel.ParcelExtensions;
import eu.jonahbauer.qed.model.parcel.LambdaCreator;
import eu.jonahbauer.qed.model.parcel.ParcelableEnum;
import eu.jonahbauer.qed.model.util.ParsedLocalDate;
import eu.jonahbauer.qed.model.util.ParsedInstant;
import eu.jonahbauer.qed.util.TextUtils;
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
    private Person.Gender personGender;
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

        for (var contact : person.getContacts()) {
            if (contact.getLabel().equalsIgnoreCase(ContactDetailType.MOBILE_PHONE_LABEL)) {
                personPhone = contact.getValue();
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
        dest.writeTypedObject(status, flags);
        dest.writeValue(organizer);

        dest.writeLong(eventId);
        dest.writeString(eventTitle);

        dest.writeLong(personId);
        dest.writeString(personName);
        dest.writeTypedObject(personBirthday, flags);
        dest.writeTypedObject(personGender, flags);
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

    @SuppressLint("ParcelClassLoader")
    public static final Creator<Registration> CREATOR = new LambdaCreator<>(Registration[]::new, source -> {
        Registration registration = new Registration(source.readLong());

        registration.status = source.readTypedObject(Status.CREATOR);
        registration.organizer = (Boolean) source.readValue(null);

        registration.eventId = source.readLong();
        registration.eventTitle = source.readString();

        registration.personId = source.readLong();
        registration.personName = source.readString();
        registration.personBirthday = source.readTypedObject(ParsedLocalDate.CREATOR);
        registration.personGender = source.readTypedObject(Person.Gender.CREATOR);
        registration.personMail = source.readString();
        registration.personAddress = source.readString();
        registration.personPhone = source.readString();

        registration.timeOfArrival = source.readTypedObject(ParsedInstant.CREATOR);
        registration.timeOfDeparture = source.readTypedObject(ParsedInstant.CREATOR);
        registration.sourceStation = source.readString();
        registration.targetStation = source.readString();
        registration.railcard = source.readString();
        registration.overnightStays = (Integer) source.readValue(null);

        registration.food = source.readString();
        registration.talks = source.readString();
        registration.notes = source.readString();

        registration.paymentAmount = (Double) source.readValue(null);
        registration.paymentDone = (Boolean) source.readValue(null);
        registration.paymentTime = source.readTypedObject(ParsedLocalDate.CREATOR);
        registration.memberAbatement = (Boolean) source.readValue(null);
        registration.otherAbatement = source.readString();

        registration.loaded = ParcelExtensions.readInstant(source);

        return registration;
    });

    @Getter
    @RequiredArgsConstructor
    public enum Status implements ParcelableEnum {
        PENDING(R.string.registration_status_pending, R.drawable.ic_event_registration_pending, R.drawable.ic_registration_status_pending),
        CONFIRMED(R.string.registration_status_confirmed, R.drawable.ic_event_registration_confirmed, R.drawable.ic_registration_status_confirmed),
        REJECTED(R.string.registration_status_rejected, R.drawable.ic_event_registration_rejected, R.drawable.ic_registration_status_rejected),
        CANCELLED(R.string.registration_status_cancelled, R.drawable.ic_event_registration_cancelled, R.drawable.ic_registration_status_cancelled);
        public static final Parcelable.Creator<Status> CREATOR = new ParcelableEnum.Creator<>(Status.values(), Status[]::new);

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
