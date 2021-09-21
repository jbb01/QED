package com.jonahbauer.qed.crypt;

import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import java.util.Set;

public interface EncryptedSharedPreferences extends SharedPreferences {
    @Override
    Editor edit();

    byte[] getByteArray(String key, byte[] defValue);

    interface Editor extends SharedPreferences.Editor {
        @Override
        Editor putString(String key, @Nullable String value);

        @Override
        Editor putStringSet(String key, @Nullable Set<String> values);

        @Override
        Editor putInt(String key, int value);

        @Override
        Editor putLong(String key, long value);

        @Override
        Editor putFloat(String key, float value);

        @Override
        Editor putBoolean(String key, boolean value);

        @Override
        Editor remove(String key);

        @Override
        Editor clear();

        Editor putByteArray(String key, @Nullable byte[] value);
    }
}
