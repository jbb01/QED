package eu.jonahbauer.qed.model.parcel;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;

import java.util.EnumSet;
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

    class Creator<T extends Enum<T> & ParcelableEnum> implements Parcelable.Creator<T> {
        private final @NonNull Class<T> clazz;
        private final @NonNull T[] values;
        private final @NonNull IntFunction<T[]> newArray;

        public Creator(@NonNull T[] values, @NonNull IntFunction<T[]> newArray) {
            this.values = values;
            this.newArray = newArray;
            //noinspection ConstantConditions,unchecked
            this.clazz = (Class<T>) values.getClass().getComponentType();
            if (values.length > 64) throw new IllegalArgumentException("ParcelableEnum only supports up to 64 enum values.");
        }

        @Override
        public T createFromParcel(Parcel source) {
            return values[source.readInt()];
        }

        public EnumSet<T> createEnumSet(long values) {
            var out = EnumSet.noneOf(clazz);

            for (int i = 0; values != 0; values >>>= 1, i++) {
                if ((values & 1) == 1) {
                    out.add(this.values[i]);
                }
            }

            return out;
        }

        @Override
        public T[] newArray(int size) {
            return newArray.apply(size);
        }
    }
}
