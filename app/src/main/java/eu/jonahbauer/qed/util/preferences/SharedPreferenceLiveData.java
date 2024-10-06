package eu.jonahbauer.qed.util.preferences;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import eu.jonahbauer.android.preference.annotations.serializer.PreferenceSerializationException;
import eu.jonahbauer.android.preference.annotations.serializer.PreferenceSerializer;

public final class SharedPreferenceLiveData<T> extends LiveData<T> {
    private static final PreferenceSerializer<?, ?> IDENTITY = new PreferenceSerializer<>() {
        @Override
        public Object serialize(Object o) throws PreferenceSerializationException {
            return o;
        }

        @Override
        public Object deserialize(Object o) throws PreferenceSerializationException {
            return o;
        }
    };

    public static <T> @NonNull SharedPreferenceLiveData<T> forString(
            @NonNull SharedPreferences preferences, @NonNull PreferenceSerializer<T, String> serializer,
            @NonNull String key, @Nullable String defaultValue
    ) {
        return new SharedPreferenceLiveData<>(preferences, SharedPreferences::getString, serializer, key, defaultValue);
    }

    public static @NonNull SharedPreferenceLiveData<String> forString(
            @NonNull SharedPreferences preferences, @NonNull String key, @Nullable String defaultValue
    ) {
        @SuppressWarnings("unchecked")
        var serializer = (PreferenceSerializer<String, String>) IDENTITY;
        return new SharedPreferenceLiveData<>(preferences, SharedPreferences::getString, serializer, key, defaultValue);
    }

    private final @NonNull String mKey;
    private final @NonNull SharedPreferences mPreferences;
    private final @NonNull OnSharedPreferenceChangeListener mListener;

    private <S> SharedPreferenceLiveData(
            @NonNull SharedPreferences preferences, @NonNull PreferenceAccessor<S> getter, @NonNull PreferenceSerializer<T, S> serializer,
            @NonNull String key, @Nullable S defaultValue
    ) {
        this.mKey = key;
        this.mPreferences = preferences;
        this.mListener = (sharedPreferences, k) -> {
            if (!mKey.equals(k)) return;
            var stored = getter.get(sharedPreferences, k, defaultValue);
            var value = serializer.deserialize(stored);
            setValue(value);
        };
    }

    @Override
    @MainThread
    protected void onActive() {
        super.onActive();
        mPreferences.registerOnSharedPreferenceChangeListener(mListener);
        mListener.onSharedPreferenceChanged(mPreferences, mKey);
    }

    @Override
    @MainThread
    protected void onInactive() {
        mPreferences.unregisterOnSharedPreferenceChangeListener(mListener);
        super.onInactive();
    }

    private interface PreferenceAccessor<T> {
        T get(SharedPreferences preference, String key, T defaultValue);
    }
}
