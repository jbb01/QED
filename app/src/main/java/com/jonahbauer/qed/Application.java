package com.jonahbauer.qed;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import org.jetbrains.annotations.Contract;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.Set;

public class Application extends android.app.Application implements android.app.Application.ActivityLifecycleCallbacks {
    public static final String LOG_TAG_ERROR = "com.jonahbauer.qed::error";
    @SuppressWarnings("unused")
    public static final String LOG_TAG_DEBUG = "com.jonahbauer.qed::debug";

    public static final String KEY_USERNAME = "username";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_USERID = "userid";

    public static final String KEY_CHAT_PWHASH = "chat_pwhash";

    public static final String KEY_GALLERY_PWHASH = "gallery_pwhash";
    public static final String KEY_GALLERY_PHPSESSID = "gallery_phpsessid";

    public static final String KEY_DATABASE_SESSION_COOKIE = "db_sessioncookie";
    public static final String KEY_DATABASE_SESSIONID = "db_sessionid";
    public static final String KEY_DATABASE_SESSIONID2 = "db_sessionid2";

    @SuppressWarnings("unused")
    public static final String KEY_FCM_DEVICE_TOKEN = "fcmDeviceToken";
    @SuppressWarnings("unused")
    public static final String KEY_FCM_RSA_KEY = "fcmRsaKeyPair";

    public static final String NOTIFICATION_CHANNEL_ID = "qednotification";


    private static final String ENCRYPTED_SHARED_PREFERENCE_FILE = "com.jonahbauer.qed_encrypted";


    public static boolean online;
    private static boolean foreground;

    private static SoftReference<Application> context;
    private SharedPreferences sharedPreferences;
    private Activity activity;
    private final Set<Activity> existingActivities;
    private ConnectionStateMonitor connectionStateMonitor;

    private String masterKeyAlias;

    {
        existingActivities = new HashSet<>();
    }

    /**
     * Loads data from the default shared preferences.
     *
     * @param key a key mapping to the data entry
     * @param encrypted should be the same value as used when saving the data entry
     * @return the stored value, null if none is stored
     */
    public String loadData(String key, boolean encrypted) {
        if (encrypted) {
            try {
                KeyGenParameterSpec keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC;
                masterKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec);
            } catch (GeneralSecurityException | IOException e) {
                Toast.makeText(this, R.string.master_key_error, Toast.LENGTH_SHORT).show();
            }

            if (masterKeyAlias == null) {
                Log.e(LOG_TAG_ERROR, "masterKeyAlias is null");
                return null;
            }

            try {
                SharedPreferences sharedPreferences = EncryptedSharedPreferences
                        .create(
                                ENCRYPTED_SHARED_PREFERENCE_FILE,
                                masterKeyAlias,
                                this,
                                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                        );
                return sharedPreferences.getString(key, null);
            } catch (GeneralSecurityException | IOException e) {
                Log.e(LOG_TAG_ERROR, e.getMessage(), e);
                return null;
            }
        } else {
            return sharedPreferences.getString(key, null);
        }
    }

    /**
     * Stores data to the default shared preferences.
     *
     * When {@param encrypted} is true the data will be encrypted using a key from the Android Keystore.
     * That way it is almost impossible to extract the value from the shared preferences even with root permission.
     *
     * @param data the data value
     * @param key a key mapping to the data entry
     * @param encrypted should be the same value as used when saving the data entry
     */
    public void saveData(String data, String key, boolean encrypted) {
        if (encrypted) {
            try {
                KeyGenParameterSpec keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC;
                masterKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec);
            } catch (GeneralSecurityException | IOException e) {
                Toast.makeText(this, "Passwort konnte nicht gespeichert werden.", Toast.LENGTH_SHORT).show();
            }
            if (masterKeyAlias == null) {
                Log.e(LOG_TAG_ERROR, "masterKeyAlias is null");
                return;
            }
            try {
                SharedPreferences sharedPreferences = EncryptedSharedPreferences
                        .create(
                                ENCRYPTED_SHARED_PREFERENCE_FILE,
                                masterKeyAlias,
                                this,
                                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                        );

                sharedPreferences.edit().putString(key, data).apply();
            } catch (GeneralSecurityException | IOException e) {
                Log.e(LOG_TAG_ERROR, e.getMessage(), e);
            }
        } else {
            sharedPreferences.edit().putString(key, data).apply();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);

        context = new SoftReference<>(this);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        connectionStateMonitor = new ConnectionStateMonitor(this);
        online = false;
        connectionStateMonitor.enable();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (connectionStateMonitor != null) connectionStateMonitor.disable();
        unregisterActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
        this.activity = activity;
        existingActivities.add(activity);
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        this.activity = activity;
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        this.activity = activity;
        foreground = true;
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        foreground = false;
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        existingActivities.remove(activity);
    }

    @Contract(pure = true)
    @NonNull
    public static SoftReference<Application> getApplicationReference() {
        return context;
    }

    /**
     * Sets the online status of the app and calls all {@code NetworkListener}.
     * @param online the new online status
     */
    public void setOnline(boolean online) {
        if (!Application.online && online) {
            if (activity!=null&&(activity instanceof NetworkListener)) ((NetworkListener)activity).onConnectionRegain();
        } else if (Application.online && !online) {
            if (activity!=null&&(activity instanceof NetworkListener)) ((NetworkListener)activity).onConnectionFail();
        } else if (!Application.online) {
            if (activity!=null&&(activity instanceof NetworkListener)) ((NetworkListener)activity).onConnectionFail();
        }
        Application.online = online;
    }

    public void finish() {
        for (Activity activity : existingActivities) activity.finish();
    }

    /**
     * Returns {@code true} if the app is in foreground.
     * When switching activities for a short time it can happen that this method returns {@code false} even though the app is in foreground.
     *
     * @return true if the app is in foreground.
     */
    @Contract(pure = true)
    public static boolean isForeground() {
        return foreground;
    }


    /**
     * Creates a notification channel for android versions greater than Oreo
     */
    public static void createNotificationChannel(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.notification_channel_name);
            String description = context.getString(R.string.notification_channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null)
                notificationManager.createNotificationChannel(channel);
        }
    }
}
