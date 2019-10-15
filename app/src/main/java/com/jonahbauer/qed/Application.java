package com.jonahbauer.qed;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class Application extends android.app.Application implements android.app.Application.ActivityLifecycleCallbacks {
    public static boolean internetConnection;
    private static Context context;
    private Activity activity;
    private CheckConnectionTask ping;
    private final Set<Activity> activeActivities;
    private boolean active;

    {
        activeActivities = new HashSet<>();
    }

    public void setLastOnlineStatus(boolean lastOnlineStatus) {
        ping.lastOnlineStatus = lastOnlineStatus;
    }

    public String loadData(String key, boolean encrypted) {
        if (encrypted) {
            try {
                // TODO require user authentication
                String ivPwHash = getSharedPreferences(getString(R.string.preferences_shared_preferences), MODE_PRIVATE).getString(key, "");
                String ivBase64 = ivPwHash.split("///")[0];
                String pwHash = ivPwHash.split("///")[1];
                byte[] pwHashBytes = Base64.decode(pwHash, Base64.NO_WRAP);
                byte[] iv = Base64.decode(ivBase64, Base64.NO_WRAP);

                KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
                keyStore.load(null);
                SecretKey secretKey = (SecretKey) keyStore.getKey(key, null);
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
                cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));

                return new String(cipher.doFinal(pwHashBytes));
            } catch (InvalidAlgorithmParameterException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | NoSuchPaddingException | InvalidKeyException | IOException | BadPaddingException | IllegalBlockSizeException e) {
                Log.e("", e.getMessage(), e);
                return "";
            }
        } else {
            return getSharedPreferences(getString(R.string.preferences_shared_preferences), MODE_PRIVATE).getString(key, "");
        }
    }

    public void saveData(String data, String key, boolean encrypted) {
        if (encrypted) {
            try {
                KeyGenerator keyGenerator = KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");

                KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                        key,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT);

                builder
                        .setKeySize(256)
                        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7);

                keyGenerator.init(builder.build());
                SecretKey secretKey = keyGenerator.generateKey();

                KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");

                keyStore.load(null);

                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");

                cipher.init(Cipher.ENCRYPT_MODE, secretKey);
                byte[] encryptedBytes = cipher.doFinal(data.getBytes());
                String pwHashEncrypted = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP);
                String iv = Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP);

                getSharedPreferences(getString(R.string.preferences_shared_preferences), MODE_PRIVATE).edit().putString(key, iv + "///" + pwHashEncrypted).apply();

            } catch (CertificateException | IOException | NoSuchAlgorithmException | KeyStoreException | InvalidAlgorithmParameterException | NoSuchProviderException | NoSuchPaddingException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                Log.e("", e.getMessage(), e);
            }
        } else {
            getSharedPreferences(getString(R.string.preferences_shared_preferences), MODE_PRIVATE).edit().putString(key, data).apply();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
        ping = new CheckConnectionTask();
        ping.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        context = this;

        Log.d("test", PreferenceManager.getDefaultSharedPreferencesName(context));
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
        if (ping !=null) ping.cancel(true);
        unregisterActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        this.activity = activity;
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
    }

    private void onApplicationResumed() {
    }

    private void onApplicationPaused() {
    }

    public static Context getContext() {
        return context;
    }

    @SuppressLint("StaticFieldLeak")
    class CheckConnectionTask extends AsyncTask<Void, Boolean, Boolean> {
        boolean lastOnlineStatus = true;
        int fails = 0;

        @Override
        protected Boolean doInBackground(Void... params) {
            while (!isCancelled()) {
                if (!isOnline()) {
                    fails++;
                } else {
                    fails = 0;
                    publishProgress(true);
                }
                if (fails>2) publishProgress(false);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return false;
        }

        @Override
        protected void onProgressUpdate(Boolean... values) {
            super.onProgressUpdate(values);
            if (!values[0]&&lastOnlineStatus) {
                if (activity!=null&&(activity instanceof Internet)) ((Internet)activity).onConnectionFail();
            } else if (values[0]&&!lastOnlineStatus) {
                if (activity!=null&&(activity instanceof Internet)) ((Internet)activity).onConnectionRegain();
            }
            internetConnection = values[0];
            lastOnlineStatus = values[0];
        }

        private boolean isOnline() {
            Runtime runtime = Runtime.getRuntime();
            try {
                Process ipProcess = runtime.exec("/system/bin/ping -c 1 "+getString(R.string.chat_server_ping));
                int exitValue = ipProcess.waitFor();
                return (exitValue == 0);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            return false;
        }
    }
}
