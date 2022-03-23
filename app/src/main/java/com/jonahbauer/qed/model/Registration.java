package com.jonahbauer.qed.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import com.jonahbauer.qed.R;

import com.jonahbauer.qed.model.parcel.ParcelExtensions;
import lombok.Data;

@Data
public class Registration implements Parcelable {
    private final long id;

    private Status status;
    private boolean organizer;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        ParcelExtensions.writeEnum(dest, status);
        ParcelExtensions.writeBoolean(dest, organizer);
    }

    public static final Creator<Registration> CREATOR = new Creator<>() {
        @Override
        public Registration createFromParcel(Parcel in) {
            Registration registration = new Registration(in.readLong());
            registration.status = ParcelExtensions.readEnum(in, Status.values());
            registration.organizer = ParcelExtensions.readBoolean(in);
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
    }
}
