package eu.jonahbauer.qed.activities.settings;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.activities.MainActivity;
import eu.jonahbauer.qed.model.Language;
import eu.jonahbauer.qed.util.Colors;
import eu.jonahbauer.qed.util.Preferences;

public class GeneralPreferenceFragment extends AbstractPreferenceFragment implements PreferenceFragment, Preference.OnPreferenceChangeListener {
    private ListPreference language;

    @Override
    public void onStart() {
        super.onStart();
        // need to overwrite saved action bar color after changing from/to dark mode
        ((MainActivity) requireActivity()).setActionBarColor(Colors.getPrimaryColor(requireContext()));
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_general, rootKey);

        language = findPreference(Preferences.getGeneral().getKeys().getLanguage());
        assert language != null;
        language.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        if (preference == language) {
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
