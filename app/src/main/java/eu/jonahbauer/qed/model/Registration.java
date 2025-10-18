package eu.jonahbauer.qed.model;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import eu.jonahbauer.qed.R;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

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
public class Registration implements Parcelable, HasId {
    public static final long NO_ID = Long.MIN_VALUE;
    private final long id;

    private @Nullable Status status;
    private @Nullable Boolean organizer;

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
    private Boolean paymentDone;
    private Boolean memberAbatement;
    private String otherAbatement;
    private final Set<Payment> payments = new LinkedHashSet<>();

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
        return paymentDone != null
                || memberAbatement != null
                || !TextUtils.isNullOrBlank(otherAbatement)
                || !payments.isEmpty();
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

        dest.writeValue(paymentDone);
        dest.writeValue(memberAbatement);
        dest.writeString(otherAbatement);
        ParcelExtensions.writeTypedCollection(dest, payments);

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

        registration.paymentDone = (Boolean) source.readValue(null);
        registration.memberAbatement = (Boolean) source.readValue(null);
        registration.otherAbatement = source.readString();
        ParcelExtensions.readTypedCollection(source, registration.payments, Payment.CREATOR);

        registration.loaded = ParcelExtensions.readInstant(source);

        return registration;
    });

    @Getter
    @RequiredArgsConstructor
    public enum Status implements ParcelableEnum {
        UNKNOWN(
                R.string.registration_status_unknown,
                R.drawable.ic_event_registration_unknown,
                R.drawable.ic_person_registration_unknown,
                R.drawable.ic_registration_status_unknown
        ),
        REJECTED(
                R.string.registration_status_rejected,
                R.drawable.ic_event_registration_rejected,
                R.drawable.ic_person_registration_rejected,
                R.drawable.ic_registration_status_rejected
        ),
        CANCELLED(
                R.string.registration_status_cancelled,
                R.drawable.ic_event_registration_cancelled,
                R.drawable.ic_person_registration_cancelled,
                R.drawable.ic_registration_status_cancelled
        )
        ;
        public static final Parcelable.Creator<Status> CREATOR = new ParcelableEnum.Creator<>(Status.values(), Status[]::new);

        private final @StringRes   int stringRes;
        private final @DrawableRes int eventDrawableRes;
        private final @DrawableRes int personDrawableRes;
        private final @DrawableRes int registrationDrawableRes;
    }

    @Data
    public static class Payment implements Parcelable {
        private ParsedLocalDate date;
        private Double amount;

        private Type type;
        private Category category;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeTypedObject(date, flags);
            dest.writeTypedObject(type, flags);
            dest.writeValue(amount);
        }

        @SuppressLint("ParcelClassLoader")
        public static final Creator<Payment> CREATOR = new LambdaCreator<>(Payment[]::new, source -> {
            var payment = new Payment();
            payment.setDate(source.readTypedObject(ParsedLocalDate.CREATOR));
            payment.setType(source.readTypedObject(Type.CREATOR));
            payment.setAmount((Double) source.readValue(null));
            return payment;
        });

        @Getter
        @RequiredArgsConstructor
        public enum Type implements ParcelableEnum {
            TRANSFER(R.string.registration_payment_type_transfer),
            EXPENSE(R.string.registration_payment_type_expense),
            ;

            public static final Parcelable.Creator<Type> CREATOR = new ParcelableEnum.Creator<>(Type.values(), Type[]::new);

            private final @StringRes int stringRes;
        }

        @Getter
        @RequiredArgsConstructor
        public enum Category implements ParcelableEnum {
            SHOPPING(R.string.registration_payment_category_shopping),
            TALK_GIFT(R.string.registration_payment_category_talk_gift),
            DEPOSIT(R.string.registration_payment_category_deposit),
            ORGANIZER_GIFT(R.string.registration_payment_category_organizer_gift),
            TRANSPORTATION(R.string.registration_payment_category_transportation),
            TALK_MATERIALS(R.string.registration_payment_category_talk_materials),
            ;

            public static final Parcelable.Creator<Category> CREATOR = new ParcelableEnum.Creator<>(Category.values(), Category[]::new);

            private final @StringRes int stringRes;
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
