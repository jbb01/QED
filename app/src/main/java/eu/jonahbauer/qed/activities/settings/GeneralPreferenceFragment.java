package eu.jonahbauer.qed.activities.settings;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;
import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.activities.MainActivity;
import eu.jonahbauer.qed.model.Language;
import eu.jonahbauer.qed.networking.NetworkConstants;
import eu.jonahbauer.qed.util.Actions;
import eu.jonahbauer.qed.util.Colors;
import eu.jonahbauer.qed.util.Preferences;

public class GeneralPreferenceFragment extends AbstractPreferenceFragment implements PreferenceFragment, Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {
    private Preference bugReport;
    private Preference github;
    private SwitchPreference nightMode;
    private ListPreference language;

    private SwitchPreference updateCheckEnabled;
    private SwitchPreference updateCheckIncludesPrereleases;

    @Override
    public void onStart() {
        super.onStart();
        // need to overwrite saved action bar color after changing from/to dark mode
        ((MainActivity) requireActivity()).setActionBarColor(Colors.getPrimaryColor(requireContext()));
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_general, rootKey);

        bugReport = findPreference(Preferences.getGeneral().getKeys().getBugReport());
        assert bugReport != null;
        bugReport.setOnPreferenceClickListener(this);

        github = findPreference(Preferences.getGeneral().getKeys().getGithub());
        assert github != null;
        github.setOnPreferenceClickListener(this);

        language = findPreference(Preferences.getGeneral().getKeys().getLanguage());
        assert language != null;
        language.setOnPreferenceChangeListener(this);

        nightMode = findPreference(Preferences.getGeneral().getKeys().getNightMode());
        assert nightMode != null;
        nightMode.setOnPreferenceChangeListener(this);

        updateCheckEnabled = findPreference(Preferences.getGeneral().getKeys().getUpdateCheckEnabled());
        assert updateCheckEnabled != null;
        updateCheckEnabled.setOnPreferenceChangeListener(this);

        updateCheckIncludesPrereleases = findPreference(Preferences.getGeneral().getKeys().getUpdateCheckIncludesPrereleases());
        assert updateCheckIncludesPrereleases != null;
        updateCheckIncludesPrereleases.setEnabled(updateCheckEnabled.isChecked());
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
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        if (preference == nightMode) {
            if (!(newValue instanceof Boolean)) return false;
            boolean value = (Boolean) newValue;

            if (value) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        } else if (preference == updateCheckEnabled) {
            if (!(newValue instanceof Boolean)) return false;
            boolean value = (Boolean) newValue;
            updateCheckIncludesPrereleases.setEnabled(value);
        } else if (preference == language) {
            Language value;
            try {
                value = Language.valueOf((String) newValue);
            } catch (ClassCastException | IllegalArgumentException e) {
                value = Language.SYSTEM;
            }
            AppCompatDelegate.setApplicationLocales(value.getLocales());
        }

        return true;
    }

    @Override
    public int getTitle() {
        return R.string.preferences_header_general;
    }
}
