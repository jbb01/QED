package com.jonahbauer.qed;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.security.keystore.KeyGenParameterSpec;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
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


    private static final String ENCRYPTED_SHARED_PREFERENCE_FILE = "com.jonahbauer.qed_encrypted";


    public static boolean online;

    private static Application context;
    private SharedPreferences sharedPreferences;
    private Activity activity;
    private final Set<Activity> activeActivities;
    private final Set<Activity> existingActivities;
    private boolean active;
    private ConnectionStateMonitor connectionStateMonitor;

    private String masterKeyAlias;

    {
        activeActivities = new HashSet<>();
        existingActivities = new HashSet<>();
    }

    public String loadData(String key, boolean encrypted) {
        if (encrypted) {
            try {
                KeyGenParameterSpec keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC;
                masterKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec);
            } catch (GeneralSecurityException | IOException e) {
                Toast.makeText(this, "Passwort konnte nicht gespeichert werden.", Toast.LENGTH_SHORT).show();
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
                                context,
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
                                context,
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

        context = this;
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
        if (activeActivities.size() == 0) {
            active = true;
            new Handler().postDelayed(() -> {
                if (active) onApplicationResumed();
            },500);
        }
        activeActivities.add(activity);
        this.activity = activity;
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        activeActivities.remove(activity);
        if (activeActivities.size() == 0) {
            active = false;
            new Handler().postDelayed(() -> {
                if (!active) onApplicationPaused();
            },500);
        }
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        existingActivities.remove(activity);
    }

    private void onApplicationResumed() {
    }

    private void onApplicationPaused() {
    }

    public static Application getContext() {
        return context;
    }

    public void setOnline(boolean online) {
        if (!Application.online && online) {
            if (activity!=null&&(activity instanceof Internet)) ((Internet)activity).onConnectionRegain();
        } else if (Application.online && !online) {
            if (activity!=null&&(activity instanceof Internet)) ((Internet)activity).onConnectionFail();
        } else if (!Application.online) {
            if (activity!=null&&(activity instanceof Internet)) ((Internet)activity).onConnectionFail();
        }
        Application.online = online;
    }

    public void finish() {
        for (Activity activity : existingActivities) activity.finish();
    }
}
