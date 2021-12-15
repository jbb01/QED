package com.jonahbauer.qed.crypt;

import android.util.Pair;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PasswordStorage {
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";

    private static EncryptedSharedPreferences mSharedPreferences;

    private static final ReentrantLock lock = new ReentrantLock();
    private static final Condition ready = lock.newCondition();

    public void init(EncryptedSharedPreferences sharedPreferences) {
        if (mSharedPreferences != null) throw new IllegalStateException("PasswordStorage is already initialized.");

        lock.lock();
        try {
            mSharedPreferences = sharedPreferences;
            ready.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public boolean saveUsernameAndPassword(String username, char[] password) {
        awaitInit();

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
        awaitInit();

        String username = mSharedPreferences.getString(KEY_USERNAME, null);
        char[] password = Optional.ofNullable(mSharedPreferences.getByteArray(KEY_PASSWORD, null))
                                  .map(PasswordUtils::toChars)
                                  .orElse(null);

        return Pair.create(username, password);
    }

    public boolean clearCredentials() {
        awaitInit();

        return mSharedPreferences.edit()
                                 .remove(KEY_USERNAME)
                                 .remove(KEY_PASSWORD)
                                 .commit();
    }

    private void awaitInit() {
        if (mSharedPreferences == null) {
            lock.lock();
            try {
                while (mSharedPreferences == null) {
                    ready.await();
                }
            } catch (InterruptedException e) {
                var e2 = new IllegalStateException("PasswordStorage is not initialized.");
                e2.addSuppressed(e);
                throw e2;
            } finally {
                lock.unlock();
            }
        }
    }
}
