package com.jonahbauer.qed.model.parcel;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;
import java.util.function.IntFunction;

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
