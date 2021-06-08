package com.jonahbauer.qed.activities.settings;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import com.jonahbauer.qed.R;

public class RootPreferenceFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_root, rootKey);
    }
}
