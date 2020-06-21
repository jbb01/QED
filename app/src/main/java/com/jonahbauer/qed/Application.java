package com.jonahbauer.qed;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.widget.Toast;

import androidx.annotation.ColorInt;
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


    public static boolean sOnline;
    private static boolean sForeground;

    private static SoftReference<Application> sContext;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences mEncryptedSharedPreferences;

    private Activity mActivity;
    private final Set<Activity> mExistingActivities;
    private ConnectionStateMonitor mConnectionStateMonitor;

    {
        mExistingActivities = new HashSet<>();
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
                loadEncryptedSharedPreferences();

                return mEncryptedSharedPreferences.getString(key, null);
            } catch (NullPointerException e) {
                return null;
            }
        } else {
            return mSharedPreferences.getString(key, null);
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
                loadEncryptedSharedPreferences();

                mEncryptedSharedPreferences.edit().putString(key, data).apply();
            } catch (NullPointerException ignored) {}
        } else {
            mSharedPreferences.edit().putString(key, data).apply();
        }
    }

    private void loadEncryptedSharedPreferences() {
        if (this.mEncryptedSharedPreferences != null) return;

        try {
            KeyGenParameterSpec keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC;
            String masterKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec);

            this.mEncryptedSharedPreferences = EncryptedSharedPreferences
                    .create(
                            ENCRYPTED_SHARED_PREFERENCE_FILE,
                            masterKeyAlias,
                            this,
                            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    );
        } catch (GeneralSecurityException | IOException e) {
            Toast.makeText(this, R.string.master_key_error, Toast.LENGTH_SHORT).show();
            throw new NullPointerException();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);

        Pref.init(getResources());

        sContext = new SoftReference<>(this);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        mConnectionStateMonitor = new ConnectionStateMonitor(this);
        sOnline = false;
        mConnectionStateMonitor.enable();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (mConnectionStateMonitor != null) mConnectionStateMonitor.disable();
        unregisterActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
        this.mActivity = activity;
        mExistingActivities.add(activity);
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        this.mActivity = activity;
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        this.mActivity = activity;
        setForeground(true);
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        setForeground(false);
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        mExistingActivities.remove(activity);
    }

    @Contract(pure = true)
    @NonNull
    public static SoftReference<Application> getApplicationReference() {
        return sContext;
    }

    /**
     * Sets the online status of the app and calls all {@code NetworkListener}.
     * @param online the new online status
     */
    public void setOnline(boolean online) {
        if (!Application.sOnline && online) {
            if (mActivity !=null&&(mActivity instanceof NetworkListener)) ((NetworkListener) mActivity).onConnectionRegain();
        } else if (Application.sOnline && !online) {
            if (mActivity !=null&&(mActivity instanceof NetworkListener)) ((NetworkListener) mActivity).onConnectionFail();
        } else if (!Application.sOnline) {
            if (mActivity !=null&&(mActivity instanceof NetworkListener)) ((NetworkListener) mActivity).onConnectionFail();
        }
        Application.sOnline = online;
    }

    /**
     * Returns the currently active Activity.
     * @return the currently active Activity
     */
    public Activity getActivity() {
        return mActivity;
    }

    public void finish() {
        for (Activity activity : mExistingActivities) activity.finish();
    }

    /**
     * Returns {@code true} if the app is in foreground.
     * When switching activities for a short time it can happen that this method returns {@code false} even though the app is in foreground.
     *
     * @return true if the app is in foreground.
     */
    @Contract(pure = true)
    public static boolean isForeground() {
        return sForeground;
    }

    public static void setForeground(boolean foreground) {
        if (Application.sForeground == foreground) return;

        Application.sForeground = foreground;
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
    
    @ColorInt
    public static int colorful(int value) {
        switch ((value % 10 + 10) % 10) {
            case 0:
                return Color.argb(255, 0x33, 0xb5, 0xe5);
            case 1:
                return Color.argb(255, 0x99, 0xcc, 0x00);
            case 2:
                return Color.argb(255, 0xff, 0x44, 0x44);
            case 3:
                return Color.argb(255, 0x00, 0x99, 0xcc);
            case 4:
                return Color.argb(255, 0x66, 0x99, 0x00);
            case 5:
                return Color.argb(255, 0xcc, 0x00, 0x00);
            case 6:
                return Color.argb(255, 0xaa, 0x66, 0xcc);
            case 7:
                return Color.argb(255, 0xff, 0xbb, 0x33);
            case 8:
                return Color.argb(255, 0xff, 0x88, 0x00);
            case 9:
                return Color.argb(255, 0x00, 0xdd, 0xff);
            default:
                throw new AssertionError("(value % 10 + 10) % 10 is not in range 0 to 9!");
        }
    }
}
