package com.jonahbauer.qed.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import com.jonahbauer.qed.R;

import lombok.Data;

@Data
public class Registration implements Parcelable {
    private final long id;

    private Event event;
    private Person person;
    private Status status;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeParcelable(event, flags);
        dest.writeParcelable(person, flags);
    }

    public static final Creator<Registration> CREATOR = new Creator<Registration>() {
        @Override
        public Registration createFromParcel(Parcel in) {
            Registration registration = new Registration(in.readLong());
            registration.event = in.readParcelable(Event.class.getClassLoader());
            registration.person = in.readParcelable(Person.class.getClassLoader());
            return registration;
        }

        @Override
        public Registration[] newArray(int size) {
            return new Registration[size];
        }
    };

    public enum Status {
        ORGA, PARTICIPATED, CONFIRMED, OPEN, CANCELLED;

        @StringRes
        public int toStringRes() {
            switch (this) {
                case ORGA:
                    return R.string.registration_status_orga;
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
                case ORGA:
                    return R.drawable.ic_event_orga;
                default:
                case OPEN:
                    return R.drawable.ic_event_member_open;
                case CONFIRMED:
                case PARTICIPATED:
                    return R.drawable.ic_event_member_confirmed;
                case CANCELLED:
                    return R.drawable.ic_event_member_opt_out;
            }
        }
    }
}
