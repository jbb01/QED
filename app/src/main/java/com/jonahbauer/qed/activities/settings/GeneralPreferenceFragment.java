package com.jonahbauer.qed.activities.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.jonahbauer.qed.BuildConfig;
import com.jonahbauer.qed.Pref;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.pingNotifications.PingNotifications;
import com.jonahbauer.qed.util.Preferences;

public class GeneralPreferenceFragment extends PreferenceFragmentCompat implements PreferenceFragment, Preference.OnPreferenceClickListener {
    private SwitchPreference pingNotifications;
    private Preference bugReport;
    private Preference github;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_general, rootKey);

        bugReport = findPreference(Preferences.general().keys().bugReport());
        bugReport.setOnPreferenceClickListener(this);

        github = findPreference(Preferences.general().keys().github());
        github.setOnPreferenceClickListener(this);

        SwitchPreference nightMode1 = findPreference(Preferences.general().keys().nightMode());
        nightMode1.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean nightMode = (boolean) newValue;
            if (nightMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            return true;
        });

        if (BuildConfig.USES_FCM) {
            pingNotifications = findPreference(Pref.FCM.PING_NOTIFICATION);
            if (pingNotifications != null)
                pingNotifications.setOnPreferenceClickListener(this);

            EditTextPreference pushPingServer = findPreference(Pref.FCM.PING_NOTIFICATION_SERVER);
            if (pushPingServer != null) {
                if (BuildConfig.DEBUG) {
                    pushPingServer.setOnPreferenceClickListener(this);
                } else {
                    pushPingServer.setEnabled(false);
                    pushPingServer.setVisible(false);
                }
            }
        }
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        if (preference == pingNotifications) {
            if (pingNotifications.isChecked()) {
                PingNotifications.getInstance(getContext()).registrationStage0();
            }
            return true;
        } else if (preference == bugReport) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_project_issue_tracker)));
            startActivity(intent);
            return true;
        } else if (preference == github) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.preferences_general_github_summary)));
            startActivity(intent);
            return true;
        }

        return false;
    }

    @Override
    public int getTitle() {
        return R.string.preferences_header_general;
    }
}
