package com.jonahbauer.qed.activities.settings;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.networking.NetworkConstants;
import com.jonahbauer.qed.util.Actions;
import com.jonahbauer.qed.util.Preferences;

public class GeneralPreferenceFragment extends PreferenceFragmentCompat implements PreferenceFragment, Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {
    private Preference bugReport;
    private Preference github;
    private SwitchPreference nightMode;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_general, rootKey);

        bugReport = findPreference(Preferences.general().keys().bugReport());
        assert bugReport != null;
        bugReport.setOnPreferenceClickListener(this);

        github = findPreference(Preferences.general().keys().github());
        assert github != null;
        github.setOnPreferenceClickListener(this);

        nightMode = findPreference(Preferences.general().keys().nightMode());
        assert nightMode != null;
        nightMode.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        if (preference == bugReport) {
            Actions.open(requireContext(), NetworkConstants.GIT_HUB_ISSUE_TRACKER);
            return true;
        } else if (preference == github) {
            Actions.open(requireContext(), NetworkConstants.GIT_HUB);
            return true;
        }

        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == nightMode) {
            if (!(newValue instanceof Boolean)) return false;
            boolean value = (Boolean) newValue;

            if (value) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        }

        return true;
    }

    @Override
    public int getTitle() {
        return R.string.preferences_header_general;
    }
}
