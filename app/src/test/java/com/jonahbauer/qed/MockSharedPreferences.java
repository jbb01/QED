package com.jonahbauer.qed;

import android.content.SharedPreferences;
import android.content.res.Resources;
import androidx.annotation.Nullable;
import com.jonahbauer.qed.util.Preferences;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Field;
import java.util.*;

import static org.mockito.ArgumentMatchers.anyInt;

public final class MockSharedPreferences implements SharedPreferences {
    private static boolean initialized;

    private final Map<String, Object> preferences = new HashMap<>();

    public synchronized static void init() {
        if (!initialized) {
            initialized = true;

            // get resource names
            Map<Integer, String> resourceNames = new HashMap<>();
            try {
                for (Field field : R.string.class.getFields()) {
                    resourceNames.put(field.getInt(null), field.getName());
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            // mock resources
            Resources resources = Mockito.mock(Resources.class);
            Mockito.when(resources.getString(anyInt())).then((Answer<String>) invocation -> {
                int arg = invocation.getArgument(0);
                return resourceNames.get(arg);
            });

            // init preferences
            Preferences.init(new MockSharedPreferences(), resources);
        } else {
            Preferences.clear();
        }
    }

    @Override
    public synchronized Map<String, ?> getAll() {
        return Map.copyOf(preferences);
    }

    @Nullable
    @Override
    public synchronized String getString(String key, @Nullable String defValue) {
        return (String) preferences.getOrDefault(key, defValue);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public synchronized Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        var set = (Set<String>) preferences.getOrDefault(key, defValues);
        return set != null ? Collections.unmodifiableSet(set) : null;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public synchronized int getInt(String key, int defValue) {
        return (int) preferences.getOrDefault(key, defValue);
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public synchronized long getLong(String key, long defValue) {
        return (long) preferences.getOrDefault(key, defValue);
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public synchronized float getFloat(String key, float defValue) {
        return (float) preferences.getOrDefault(key, defValue);
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public synchronized boolean getBoolean(String key, boolean defValue) {
        return (boolean) preferences.getOrDefault(key, defValue);
    }

    @Override
    public synchronized boolean contains(String key) {
        return preferences.containsKey(key);
    }

    @Override
    public Editor edit() {
        return new Editor();
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        throw new UnsupportedOperationException();
    }

    public class Editor implements SharedPreferences.Editor {
        private final Map<String, Object> values = new HashMap<>();
        private boolean clear;

        @Override
        public SharedPreferences.Editor putString(String key, @Nullable String value) {
            this.values.put(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor putStringSet(String key, @Nullable Set<String> values) {
            this.values.put(key, values);
            return this;
        }

        @Override
        public SharedPreferences.Editor putInt(String key, int value) {
            this.values.put(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor putLong(String key, long value) {
            this.values.put(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor putFloat(String key, float value) {
            this.values.put(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor putBoolean(String key, boolean value) {
            this.values.put(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor remove(String key) {
            this.values.put(key, null);
            return this;
        }

        @Override
        public SharedPreferences.Editor clear() {
            this.clear = true;
            this.values.clear();
            return this;
        }

        @Override
        public boolean commit() {
            synchronized (MockSharedPreferences.this) {
                if (clear) {
                    preferences.clear();
                }
                values.forEach((key, value) -> {
                    if (value == null) {
                        preferences.remove(key);
                    } else {
                        preferences.put(key, value);
                    }
                });

                return true;
            }
        }

        @Override
        public void apply() {
            commit();
        }
    }
}
