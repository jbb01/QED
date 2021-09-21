package com.jonahbauer.qed.crypt;

import android.util.Pair;

import java.util.Arrays;
import java.util.Optional;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PasswordStorage {
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";

    private EncryptedSharedPreferences mSharedPreferences;
    private boolean mInitialized;

    public void init(EncryptedSharedPreferences sharedPreferences) {
        if (mInitialized) throw new IllegalStateException("PasswordStorage is already initialized.");
        mSharedPreferences = sharedPreferences;
        mInitialized = true;
    }

    public boolean saveUsernameAndPassword(String username, char[] password) {
        if (!mInitialized) throw new IllegalStateException("PasswordStorage is not initialized.");

        try {
            mSharedPreferences.edit()
                              .putString(KEY_USERNAME, username)
                              .putByteArray(KEY_PASSWORD, PasswordUtils.toBytes(password))
                              .apply();
            return true;
        } finally {
            Arrays.fill(password, '\u0000');
        }
    }

    public Pair<String, char[]> loadUsernameAndPassword() {
        if (!mInitialized) throw new IllegalStateException("PasswordStorage is not initialized.");

        String username = mSharedPreferences.getString(KEY_USERNAME, null);
        char[] password = Optional.ofNullable(mSharedPreferences.getByteArray(KEY_PASSWORD, null))
                                  .map(PasswordUtils::toChars)
                                  .orElse(null);

        return Pair.create(username, password);
    }

    public boolean clearCredentials() {
        if (!mInitialized) throw new IllegalStateException("PasswordStorage is not initialized.");

        return mSharedPreferences.edit()
                                 .remove(KEY_USERNAME)
                                 .remove(KEY_PASSWORD)
                                 .commit();
    }
}
