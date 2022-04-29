package com.jonahbauer.qed.model.parcel;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * This class can be used to reduce the boilerplate of implementing the {@link Parcelable} interface.
 * When the implementing class has a constructor that accepts a {@link Parcel} as its single argument,
 * the {@link Parcelable.Creator} can be implemented like this
 * <pre>public static final Creator&lt;T&gt; CREATOR = new LambdaCreator&lt;&gt;(T[]::new, T::new)</pre>
 */
@RequiredArgsConstructor
public class LambdaCreator<T> implements Parcelable.Creator<T> {
    private final @NonNull IntFunction<T[]> newArray;
    private final @NonNull Function<Parcel, T> creator;

    @Override
    public T createFromParcel(Parcel source) {
        return creator.apply(source);
    }

    @Override
    public T[] newArray(int size) {
        return newArray.apply(size);
    }
}
