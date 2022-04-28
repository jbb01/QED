package com.jonahbauer.qed.model.util;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.jonahbauer.qed.model.parcel.LambdaCreator;
import com.jonahbauer.qed.model.parcel.ParcelExtensions;
import lombok.Data;
import lombok.experimental.ExtensionMethod;

import java.time.Instant;

@Data
@ExtensionMethod(ParcelExtensions.class)
public class ParsedInstant implements Comparable<ParsedInstant>, Parcelable {
    private final @NonNull String string;
    private final @Nullable Instant instant;

    @Override
    public int compareTo(ParsedInstant other) {
        if (instant != null && other.instant != null) {
            return instant.compareTo(other.instant);
        } else {
            return string.compareTo(other.string);
        }
    }

    @Override
    public @NonNull String toString() {
        if (instant != null) return instant.toString();
        else return string;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(string);
        dest.writeInstant(instant);
    }

    public static final Creator<ParsedInstant> CREATOR = new LambdaCreator<>(ParsedInstant[]::new, source -> {
        return new ParsedInstant(
                source.readString(),
                source.readInstant()
        );
    });
}
