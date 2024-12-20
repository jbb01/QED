package eu.jonahbauer.qed;

import android.accounts.*;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import eu.jonahbauer.qed.crypt.PasswordStorage;
import eu.jonahbauer.qed.ui.themes.Theme;
import eu.jonahbauer.qed.ui.themes.ThemeListener;
import eu.jonahbauer.qed.networking.cookies.QEDCookieHandler;
import eu.jonahbauer.qed.util.Preferences;

import eu.jonahbauer.qed.util.UpdateChecker;
import eu.jonahbauer.qed.util.preferences.RecentsSharedPreferenceListener;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import org.jetbrains.annotations.Contract;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.security.GeneralSecurityException;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.exceptions.UndeliverableException;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class Application extends android.app.Application implements android.app.Application.ActivityLifecycleCallbacks {
    private static final String LOG_TAG = Application.class.getName();
    private static final String COOKIE_SHARED_PREFERENCE_FILE = "eu.jonahbauer.qed_cookies";
    private static final String ENCRYPTED_SHARED_PREFERENCE_FILE = "eu.jonahbauer.qed_encrypted";

    public static int MEMORY_CLASS;

    private static SoftReference<Application> sContext;

    private Activity mActivity;
    private ConnectionStateMonitor mConnectionStateMonitor;

    private boolean mUpdateCheckPending = true;
    private final CompositeDisposable mActivityDisposable = new CompositeDisposable();

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

        initSharedPreferences();

        mConnectionStateMonitor = new ConnectionStateMonitor(this);
        mConnectionStateMonitor.enable();

        // create account if necessary
        initAccount();
    }

    private void initSharedPreferences() {
        // handle encrypted shared preferences asynchronously
        Observable.fromRunnable(() -> {
            // Setup cookie handler
            SharedPreferences cookieSharedPreferences = Application.this.getEncryptedSharedPreferences(COOKIE_SHARED_PREFERENCE_FILE);
            QEDCookieHandler.init(cookieSharedPreferences);

            // Setup password storage
            SharedPreferences passwordSharedPreferences = Application.this.getEncryptedSharedPreferences(ENCRYPTED_SHARED_PREFERENCE_FILE);
            PasswordStorage.init(passwordSharedPreferences);
        }).subscribeOn(Schedulers.computation()).subscribe();

        // setup preferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Preferences.init(preferences, this.getResources());

        // apply theme
        Theme.getCurrentTheme().apply(this);
        preferences.registerOnSharedPreferenceChangeListener(ThemeListener.INSTANCE);

        // apply language
        var language = Preferences.getGeneral().getLanguage();
        AppCompatDelegate.setApplicationLocales(language.getLocales());

        // track changes to name and channel
        preferences.registerOnSharedPreferenceChangeListener(RecentsSharedPreferenceListener.INSTANCE);
    }

    private void initAccount() {
        AccountManager accountManager = AccountManager.get(this);

        String accountType = getString(R.string.account_type);
        String accountName = getString(R.string.app_name);
        Account account = new Account(accountName, accountType);

        if (accountManager.addAccountExplicitly(account, null, null)) {
            Log.d(LOG_TAG, "Successfully added account");
        } else {
            Log.w(LOG_TAG, "Failed to add account.");
        }
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
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        this.mActivity = activity;

        if (mUpdateCheckPending) {
            mUpdateCheckPending = false;
            mActivityDisposable.add(UpdateChecker.getUpdateRelease().subscribe(release -> {
                UpdateChecker.showUpdateDialog(activity, release);
            }));
        }
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        this.mActivity = activity;
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {}

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        mActivityDisposable.clear();
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {}
    //</editor-fold>

    @Contract(pure = true)
    @NonNull
    public static SoftReference<Application> getApplicationReference() {
        return sContext;
    }

    public ConnectionStateMonitor getConnectionStateMonitor() {
        return mConnectionStateMonitor;
    }

    /**
     * Returns the currently active Activity.
     * @return the currently active Activity
     */
    public Activity getActivity() {
        return mActivity;
    }

    private SharedPreferences getEncryptedSharedPreferences(String name) {
        try {
            KeyGenParameterSpec keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC;
            String masterKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec);

            return EncryptedSharedPreferences.create(
                    name,
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
}
