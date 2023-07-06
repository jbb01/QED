package eu.jonahbauer.qed.layoutStuff.themes;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import eu.jonahbauer.qed.Application;
import eu.jonahbauer.qed.util.Preferences;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ThemeListener implements OnSharedPreferenceChangeListener {
    public static final ThemeListener INSTANCE = new ThemeListener();

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (Preferences.getGeneral().getKeys().getTheme().equals(key)) {
            var application = Application.getApplicationReference().get();
            if (application == null) return;

            var newTheme = Preferences.getGeneral().getTheme();
            newTheme.apply(application);
            Theme.setCurrentTheme(newTheme);
        }
    }
}
