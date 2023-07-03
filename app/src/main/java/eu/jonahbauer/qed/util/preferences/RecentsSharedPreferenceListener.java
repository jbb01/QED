package eu.jonahbauer.qed.util.preferences;

import android.content.SharedPreferences;
import eu.jonahbauer.qed.util.Preferences;

import java.util.LinkedHashSet;

public class RecentsSharedPreferenceListener implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final RecentsSharedPreferenceListener INSTANCE = new RecentsSharedPreferenceListener();

    private RecentsSharedPreferenceListener() {}

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (Preferences.getChat().getKeys().getName().equals(key)) {
            var name = Preferences.getChat().getName();

            var recentNames = new LinkedHashSet<>(Preferences.getChat().getRecentNames());
            recentNames.remove(name);
            recentNames.add(name);
            Preferences.getChat().setRecentNames(recentNames);
        } else if (Preferences.getChat().getKeys().getChannel().equals(key)) {
            var channel = Preferences.getChat().getChannel();

            var recentChannels = new LinkedHashSet<>(Preferences.getChat().getRecentChannels());
            recentChannels.remove(channel);
            recentChannels.add(channel);
            Preferences.getChat().setRecentChannels(recentChannels);
        }
    }
}
