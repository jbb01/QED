package com.jonahbauer.qed.activities.settings;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.sheets.album.AlbumInfoBottomSheet;
import com.jonahbauer.qed.util.Actions;
import com.jonahbauer.qed.util.Debug;

public class DebugPreferenceFragment extends PreferenceFragmentCompat implements PreferenceFragment, Preference.OnPreferenceClickListener {
    private Preference mPersonInfoSheet;
    private Preference mEventInfoSheet;
    private Preference mAlbumInfoSheet;
    private Preference mMessageInfoSheet;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_debug, rootKey);

        mPersonInfoSheet = findPreference("personInfoSheet");
        assert mPersonInfoSheet != null;
        mPersonInfoSheet.setOnPreferenceClickListener(this);

        mEventInfoSheet = findPreference("eventInfoSheet");
        assert mEventInfoSheet != null;
        mEventInfoSheet.setOnPreferenceClickListener(this);

        mAlbumInfoSheet = findPreference("albumInfoSheet");
        assert mAlbumInfoSheet != null;
        mAlbumInfoSheet.setOnPreferenceClickListener(this);

        mMessageInfoSheet = findPreference("messageInfoSheet");
        assert mMessageInfoSheet != null;
        mMessageInfoSheet.setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        if (preference == mPersonInfoSheet) {
            Actions.showInfoSheet(this, Debug.dummyPerson());
            return true;
        } else if (preference == mEventInfoSheet) {
            Actions.showInfoSheet(this, Debug.dummyEvent());
            return true;
        } else if (preference == mAlbumInfoSheet) {
            AlbumInfoBottomSheet sheet = AlbumInfoBottomSheet.newInstance(Debug.dummyAlbum());
            sheet.show(getChildFragmentManager(), sheet.getTag());
            return true;
        } else if (preference == mMessageInfoSheet) {
            Actions.showInfoSheet(this, Debug.dummyMessage());
            return true;
        }

        return false;
    }

    @Override
    public int getTitle() {
        return R.string.preferences_header_debug;
    }
}
