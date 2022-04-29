package com.jonahbauer.qed.model.parcel;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.function.IntFunction;

/**
 * Together with {@link Creator} this interface provides an easy way to implement
 * the {@link Parcelable} interface for enum types.
 * Classes implementing the {@code ParcelableEnum} interface must also have a non-null static field called CREATOR
 * of type {@link Creator}.
 * <pre>
 * public enum MyEnum implements ParcelableEnum {
 *     VALUE_A, VALUE_B, VALUE_C;
 *     public static final Creator&lt;MyEnum&gt; CREATOR = new Creator&lt;&gt;(MyEnum.values(), MyEnum[]::new);
 * }</pre>
 * @implNote The enum is parcelled using its ordinal value. When using the parcel only as intended and not sharing it
 * between code versions, this does not pose a problem.
 */
public interface ParcelableEnum extends Parcelable {
    int ordinal();

    @Override
    default int describeContents() {
        return 0;
    }

    @Override
    default void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(ordinal());
    }

    @RequiredArgsConstructor
    class Creator<T extends Enum<T> & ParcelableEnum> implements Parcelable.Creator<T> {
        private final @NonNull T[] values;
        private final @NonNull IntFunction<T[]> newArray;


        @Override
        public T createFromParcel(Parcel source) {
            return values[source.readInt()];
        }

        @Override
        public T[] newArray(int size) {
            return newArray.apply(size);
        }
    }
}
