package com.jonahbauer.qed;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;
import androidx.security.crypto.MasterKeys;

import com.jonahbauer.qed.crypt.EncryptedSharedPreferences;
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

import io.reactivex.rxjava3.exceptions.UndeliverableException;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public class Application extends android.app.Application implements android.app.Application.ActivityLifecycleCallbacks {
    private static final String LOG_TAG = Application.class.getName();
    private static final String COOKIE_SHARED_PREFERENCE_FILE = "com.jonahbauer.qed_cookies";
    private static final String ENCRYPTED_SHARED_PREFERENCE_FILE = "com.jonahbauer.qed_encrypted";

    public static int MEMORY_CLASS;

    private static SoftReference<Application> sContext;

    private Boolean mOnline = null;

    private Activity mActivity;
    private final Set<Activity> mExistingActivities = new HashSet<>();
    private ConnectionStateMonitor mConnectionStateMonitor;

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);

        sContext = new SoftReference<>(this);

        // Memory Class
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        MEMORY_CLASS = am.getMemoryClass();

        // RxJava global exception handler
        RxJavaPlugins.setErrorHandler(e -> {
            if (e instanceof UndeliverableException) {
                e = e.getCause();
            }

            if (e instanceof IOException) {
                // fine, irrelevant network problem or API that throws on cancellation
                return;
            }
            if (e instanceof InterruptedException) {
                // fine, some blocking code was interrupted by a dispose call
                return;
            }

            Log.e(LOG_TAG, "Undeliverable Exception", e);
        });

        // Setup cookie handler
        SharedPreferences cookieSharedPreferences = getEncryptedSharedPreferences(COOKIE_SHARED_PREFERENCE_FILE);
        QEDCookieHandler.init(cookieSharedPreferences);

        // Setup password storage
        EncryptedSharedPreferences passwordSharedPreferences = getEncryptedSharedPreferences(ENCRYPTED_SHARED_PREFERENCE_FILE);
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
        this.mExistingActivities.add(activity);
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        this.mActivity = activity;
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        this.mActivity = activity;
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {}

    @Override
    public void onActivityStopped(@NonNull Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        this.mExistingActivities.remove(activity);
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
    @MainThread
    public void setOnline(boolean online) {
        if (mActivity instanceof NetworkListener) {
            NetworkListener listener = (NetworkListener) mActivity;

            if (mOnline != null && !mOnline && online) listener.onConnectionRegain();
            else if ((mOnline == null || mOnline) && !online) listener.onConnectionFail();
        }

        mOnline = online;
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

    private EncryptedSharedPreferences getEncryptedSharedPreferences(String name) {
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
