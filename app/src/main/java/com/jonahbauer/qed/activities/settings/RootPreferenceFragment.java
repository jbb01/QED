package com.jonahbauer.qed.activities.settings;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.jonahbauer.qed.R;

public class RootPreferenceFragment extends PreferenceFragmentCompat {
    private static final String KEY_GENERAL = "settings_header_general";
    private static final String KEY_CHAT = "settings_header_chat";
    private static final String KEY_GALLERY = "settings_header_gallery";
    private static final String KEY_DEBUG = "settings_header_debug";

    private NavController mNavController;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNavController = NavHostFragment.findNavController(this);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_root, rootKey);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        NavDirections action = null;
        switch (preference.getKey()) {
            case KEY_GENERAL:
                action = RootPreferenceFragmentDirections.general();
                break;
            case KEY_CHAT:
                action = RootPreferenceFragmentDirections.chat();
                break;
            case KEY_GALLERY:
                action = RootPreferenceFragmentDirections.gallery();
                break;
            case KEY_DEBUG:
                action = RootPreferenceFragmentDirections.debug();
                break;
        }

        if (action != null) {
            mNavController.navigate(action);
            return true;
        } else {
            return false;
        }
    }
}
