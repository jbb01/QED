package com.jonahbauer.qed.model.parcel;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.IntFunction;

@UtilityClass
public class ParcelExtensions {
    private static final int VAL_NULL = 0;
    private static final int VAL_NON_NULL = 1;

    /**
     * Writes an {@link Instant} to a parcel. Limitations of {@link Instant#toEpochMilli()} apply.
     */
    public static void writeInstant(@NonNull Parcel dest, @Nullable Instant instant) {
        if (instant == null) {
            dest.writeInt(VAL_NULL);
        } else {
            dest.writeInt(VAL_NON_NULL);
            dest.writeLong(instant.toEpochMilli());
        }
    }

    /**
     * Reads an {@link Instant} from a parcel. Limitations of {@link Instant#toEpochMilli()} apply.
     */
    @Nullable
    public static Instant readInstant(@NonNull Parcel source) {
        var flag = source.readInt();
        if (flag == VAL_NULL) {
            return null;
        } else if (flag == VAL_NON_NULL) {
            return Instant.ofEpochMilli(source.readLong());
        } else {
            throw new RuntimeException("Could not read Instant from parcel.");
        }
    }

    /**
     * Writes a {@link LocalDate} to a parcel.
     */
    public static void writeLocalDate(@NonNull Parcel dest, @Nullable LocalDate localDate) {
        if (localDate == null) {
            dest.writeInt(VAL_NULL);
        } else {
            dest.writeInt(VAL_NON_NULL);
            dest.writeLong(localDate.toEpochDay());
        }
    }

    /**
     * Reads a {@link LocalDate} from a parcel.
     */
    @Nullable
    public static LocalDate readLocalDate(@NonNull Parcel source) {
        var flag = source.readInt();
        if (flag == VAL_NULL) {
            return null;
        } else if (flag == VAL_NON_NULL) {
            return LocalDate.ofEpochDay(source.readLong());
        } else {
            throw new RuntimeException("Could not read LocalDate from parcel.");
        }
    }

    /**
     * Writes a {@code boolean} to a parcel.
     */
    public static void writeBoolean(@NonNull Parcel dest, boolean bool) {
        dest.writeInt(bool ? 1 : 0);
    }

    /**
     * Reads a {@code boolean} from a parcel.
     */
    public static boolean readBoolean(@NonNull Parcel source) {
        return source.readInt() != 0;
    }

    /**
     * Writes an {@code enum} to a parcel.
     */
    public static <T extends Enum<T>> void writeEnum(@NonNull Parcel dest, T value) {
        if (value == null) {
            dest.writeInt(VAL_NULL);
        } else {
            dest.writeInt(VAL_NON_NULL);
            dest.writeInt(value.ordinal());
        }
    }

    /**
     * Reads an {@code enum} from a parcel.
     */
    public static <T extends Enum<T>> T readEnum(@NonNull Parcel source, IntFunction<T> getter) {
        var flag = source.readInt();
        if (flag == VAL_NULL) {
            return null;
        } else if (flag == VAL_NON_NULL) {
            return getter.apply(source.readInt());
        } else {
            throw new RuntimeException("Could not read enum from parcel.");
        }
    }

    public static <T extends Parcelable> void writeTypedCollection(@NonNull Parcel dest, Collection<T> collection) {
        if (collection == null) {
            dest.writeInt(-1);
        } else {
            var array = collection.toArray(new Parcelable[0]);
            dest.writeInt(array.length);
            if (array.length > 0) {
                dest.writeTypedArray(array, 0);
            }
        }
    }

    public static <T extends Parcelable> void readTypedCollection(@NonNull Parcel source, Collection<T> collection, Parcelable.Creator<T> creator) {
        collection.clear();

        var size = source.readInt();

        if (size > 0) {
            var array = creator.newArray(size);
            source.readTypedArray(array, creator);
            collection.addAll(Arrays.asList(array));
        }
    }

    public static void writeStringCollection(@NonNull Parcel dest, Collection<String> collection) {
        if (collection == null) {
            dest.writeInt(-1);
        } else {
            var array = collection.toArray(new String[0]);
            dest.writeInt(array.length);
            if (array.length > 0) {
                dest.writeStringArray(array);
            }
        }
    }

    public static void readStringCollection(@NonNull Parcel source, Collection<? super String> collection) {
        collection.clear();

        var size = source.readInt();

        if (size > 0) {
            var array = new String[size];
            source.readStringArray(array);
            collection.addAll(Arrays.asList(array));
        }
    }
}
