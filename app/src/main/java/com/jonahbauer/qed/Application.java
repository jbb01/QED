package com.jonahbauer.qed;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;
import androidx.security.crypto.MasterKeys;

import com.jonahbauer.qed.crypt.MyEncryptedSharedPreferences;
import com.jonahbauer.qed.crypt.PasswordStorage;
import com.jonahbauer.qed.networking.NetworkListener;
import com.jonahbauer.qed.networking.cookies.QEDCookieHandler;
import com.jonahbauer.qed.util.Preferences;

import org.jetbrains.annotations.Contract;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.Set;

public class Application extends android.app.Application implements android.app.Application.ActivityLifecycleCallbacks {

    @SuppressWarnings("unused")
    public static final String KEY_FCM_DEVICE_TOKEN = "fcmDeviceToken";
    @SuppressWarnings("unused")
    public static final String KEY_FCM_RSA_KEY = "fcmRsaKeyPair";

    public static final String NOTIFICATION_CHANNEL_ID = "qednotification";


    private static final String COOKIE_SHARED_PREFERENCE_FILE = "com.jonahbauer.qed_cookies";
    private static final String ENCRYPTED_SHARED_PREFERENCE_FILE = "com.jonahbauer.qed_encrypted";


    public static boolean sOnline;
    private static boolean sForeground;

    private static SoftReference<Application> sContext;

    private Activity mActivity;
    private final Set<Activity> mExistingActivities = new HashSet<>();
    private ConnectionStateMonitor mConnectionStateMonitor;

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);

        Pref.init(getResources());

        sContext = new SoftReference<>(this);

        // Setup cookie handler
        SharedPreferences cookieSharedPreferences = getEncryptedSharedPreferences(COOKIE_SHARED_PREFERENCE_FILE);
        QEDCookieHandler.init(cookieSharedPreferences);

        // Setup password storage
        MyEncryptedSharedPreferences passwordSharedPreferences = getEncryptedSharedPreferences(ENCRYPTED_SHARED_PREFERENCE_FILE);
        PasswordStorage.init(passwordSharedPreferences);

        // Setup preferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Preferences.init(preferences, this.getResources());

        // Activate Night Mode
        if (Preferences.general().isNightMode()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

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

    //<editor-fold desc="Activity Lifecycle Listener" defaultstate="collapsed">
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
    //</editor-fold>

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

    private MyEncryptedSharedPreferences getEncryptedSharedPreferences(String name) {
        try {
            KeyGenParameterSpec keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC;
            String masterKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec);

            return MyEncryptedSharedPreferences.create(
                    name,
                    masterKeyAlias,
                    this,
                    MyEncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    MyEncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            Toast.makeText(this, R.string.master_key_error, Toast.LENGTH_SHORT).show();
            throw new NullPointerException();
        }
    }
}
