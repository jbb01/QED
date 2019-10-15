package com.jonahbauer.qed;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.security.keystore.KeyGenParameterSpec;
import android.util.Log;
import android.widget.Toast;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.Map;
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
            return getSharedPreferences(getString(R.string.preferences_shared_preferences), MODE_PRIVATE).getString(key, null);
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
            getSharedPreferences(getString(R.string.preferences_shared_preferences), MODE_PRIVATE).edit().putString(key, data).apply();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);

        context = this;

        connectionStateMonitor = new ConnectionStateMonitor(this);
        online = false;
        connectionStateMonitor.enable();

        getSharedPreferences(getString(R.string.preferences_shared_preferences), MODE_PRIVATE).registerOnSharedPreferenceChangeListener((SharedPreferences sharedPreferences, String key) -> {
            SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            Map<String, ?> all = sharedPreferences.getAll();
            if (all.get(key) instanceof String) {
                defaultSharedPreferences.edit().putString(key,sharedPreferences.getString(key,"")).apply();
            } else if (all.get(key) instanceof Boolean) {
                defaultSharedPreferences.edit().putBoolean(key,sharedPreferences.getBoolean(key,false)).apply();
            } else if (all.get(key) instanceof Integer) {
                defaultSharedPreferences.edit().putInt(key,sharedPreferences.getInt(key,0)).apply();
            } else if (all.get(key) instanceof Float) {
                defaultSharedPreferences.edit().putFloat(key,sharedPreferences.getFloat(key,0f)).apply();
            } else if (all.get(key) instanceof Long) {
                defaultSharedPreferences.edit().putLong(key,sharedPreferences.getLong(key,0L)).apply();
            } else if (all.get(key) instanceof Set<?>) {
                defaultSharedPreferences.edit().putStringSet(key,sharedPreferences.getStringSet(key,null)).apply();
            }
        });
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (connectionStateMonitor != null) connectionStateMonitor.disable();
        unregisterActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        this.activity = activity;
        existingActivities.add(activity);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void onActivityResumed(Activity activity) {
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
    public void onActivityPaused(Activity activity) {
        activeActivities.remove(activity);
        if (activeActivities.size() == 0) {
            active = false;
            new Handler().postDelayed(() -> {
                if (!active) onApplicationPaused();
            },500);
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {
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
