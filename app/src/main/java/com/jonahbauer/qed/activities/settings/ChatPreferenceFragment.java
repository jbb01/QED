package com.jonahbauer.qed.activities.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.database.ChatDatabase;
import com.jonahbauer.qed.layoutStuff.SeekBarPreference;
import com.jonahbauer.qed.util.Preferences;

public class ChatPreferenceFragment extends PreferenceFragmentCompat implements PreferenceFragment, Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {
    private Preference deleteDatabase;
    private SwitchPreference katex;
    private SwitchPreference links;

    public static final int[] maxShownRowsValues = {10_000, 20_000, 50_000, 100_000, 200_000, 500_000, Integer.MAX_VALUE};
    private static final String[] maxShownRowsStringValues = {"10.000", "20.000", "50.000", "100.000", "200.000", "500.000", "\u221E"};

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_chat, rootKey);
        setHasOptionsMenu(true);

        katex = findPreference(Preferences.chat().keys().katex());
        katex.setOnPreferenceChangeListener(this);

        links = findPreference(Preferences.chat().keys().linkify());
        links.setOnPreferenceChangeListener(this);

        deleteDatabase = findPreference(Preferences.chat().keys().dbClear());
        deleteDatabase.setOnPreferenceClickListener(this);

        SeekBarPreference maxShownRows = findPreference(Preferences.chat().keys().dbMaxResults());
        maxShownRows.setExternalValues(maxShownRowsValues, maxShownRowsStringValues);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            startActivity(new Intent(getActivity(), RootSettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        Context context = getActivity();
        if (context == null) return false;

        if (preference == deleteDatabase) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
            alertDialog.setMessage(R.string.preferences_chat_confirm_delete_db);
            alertDialog.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
            alertDialog.setPositiveButton(R.string.delete, (dialog, which) -> {
                ChatDatabase db = new ChatDatabase();
                db.init(getContext(), null);
                db.clear();
                db.close();
            });
            alertDialog.show();
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == katex) {
            if (!(newValue instanceof Boolean)) return true;
            boolean value = (Boolean) newValue;

            if (links != null) {
                if (value) {
                    if (getContext() != null)
                        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putBoolean("oldLinkValue", links.isChecked()).apply();
                    links.setChecked(false);
                } else {
                    if (getContext() != null) {
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                        sharedPreferences.edit().putBoolean("oldKatexValue", false).apply();
                        links.setChecked(sharedPreferences.getBoolean("oldLinkValue", true));
                    }
                }
            }
        } else if (preference == links) {
            if (!(newValue instanceof Boolean)) return true;
            boolean value = (Boolean) newValue;

            if (katex != null) {
                if (value) {
                    if (getContext() != null)
                        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putBoolean("oldKatexValue", katex.isChecked()).apply();
                    katex.setChecked(false);
                } else {
                    if (getContext() != null) {
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                        sharedPreferences.edit().putBoolean("oldLinkValue", false).apply();
                        katex.setChecked(sharedPreferences.getBoolean("oldKatexValue", true));
                    }
                }
            }
        }

        return true;
    }

    @Override
    public int getTitle() {
        return R.string.preferences_header_chat;
    }
}
