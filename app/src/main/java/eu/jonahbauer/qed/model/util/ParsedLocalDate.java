package eu.jonahbauer.qed.model.util;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import eu.jonahbauer.qed.model.parcel.LambdaCreator;
import eu.jonahbauer.qed.model.parcel.ParcelExtensions;
import lombok.Data;
import lombok.experimental.ExtensionMethod;

import java.time.LocalDate;

@Data
@ExtensionMethod(ParcelExtensions.class)
public class ParsedLocalDate implements Comparable<ParsedLocalDate>, Parcelable {
    private final @NonNull String string;
    private final @Nullable LocalDate localDate;

    @Override
    public int compareTo(ParsedLocalDate other) {
        if (localDate != null && other.localDate != null) {
            return localDate.compareTo(other.localDate);
        } else {
            return string.compareTo(other.string);
        }
    }

    @Override
    public @NonNull String toString() {
        if (localDate != null) return localDate.toString();
        else return string;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(string);
        dest.writeLocalDate(localDate);
    }

    public static final Creator<ParsedLocalDate> CREATOR = new LambdaCreator<>(ParsedLocalDate[]::new, source -> {
        return new ParsedLocalDate(
                source.readString(),
                source.readLocalDate()
        );
    });
}
