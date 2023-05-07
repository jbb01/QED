package com.jonahbauer.qed.activities.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.ColorPickerDialogFragment;
import com.jonahbauer.qed.layoutStuff.SeekBarPreference;
import com.jonahbauer.qed.model.room.Database;
import com.jonahbauer.qed.util.MessageUtils;
import com.jonahbauer.qed.util.Preferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;
import com.jonahbauer.qed.util.ViewUtils;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Collections;
import java.util.stream.Collectors;

public class ChatPreferenceFragment extends AbstractPreferenceFragment implements PreferenceFragment, Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {
    private static final int[] MAX_SHOWN_ROWS_VALUES = {10_000, 20_000, 50_000, 100_000, 200_000, 500_000, Integer.MAX_VALUE};
    private static final String[] MAX_SHOWN_ROWS_STRING_VALUES = {"10.000", "20.000", "50.000", "100.000", "200.000", "500.000", "∞"};

    private Preference deleteDatabase;
    private EditTextPreference name;
    private EditTextPreference channel;
    private SwitchPreference katex;
    private SwitchPreference links;
    private Preference color;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_chat, rootKey);
        setHasOptionsMenu(true);

        name = findPreference(Preferences.getChat().getKeys().getName());
        assert name != null;
        name.setSummaryProvider(preference -> {
            var name = Preferences.getChat().getName();
            return MessageUtils.formatName(preference.getContext(), name);
        });

        channel = findPreference(Preferences.getChat().getKeys().getChannel());
        assert channel != null;
        channel.setSummaryProvider(preference -> {
            var channel = Preferences.getChat().getChannel();
            return MessageUtils.formatChannel(preference.getContext(), channel);
        });

        katex = findPreference(Preferences.getChat().getKeys().getKatex());
        assert katex != null;
        katex.setOnPreferenceChangeListener(this);

        links = findPreference(Preferences.getChat().getKeys().getLinkify());
        assert links != null;
        links.setOnPreferenceChangeListener(this);

        deleteDatabase = findPreference(Preferences.getChat().getKeys().getDeleteDb());
        assert deleteDatabase != null;
        deleteDatabase.setOnPreferenceClickListener(this);

        color = findPreference(Preferences.getChat().getKeys().getColor());
        assert color != null;
        color.setOnPreferenceClickListener(this);

        SeekBarPreference maxShownRows = findPreference(Preferences.getChat().getKeys().getDbMaxResult());
        assert maxShownRows != null;
        maxShownRows.setExternalValues(MAX_SHOWN_ROWS_VALUES, MAX_SHOWN_ROWS_STRING_VALUES);
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        Context context = getActivity();
        if (context == null) return false;

        if (preference == deleteDatabase) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
            alertDialog.setMessage(R.string.preferences_chat_confirm_delete_db);
            alertDialog.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
            alertDialog.setPositiveButton(R.string.delete, (dialog, which) -> {
                //noinspection ResultOfMethodCallIgnored
                Database.getInstance(requireContext()).messageDao().clear()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> Snackbar.make(requireView(), R.string.deleted, Snackbar.LENGTH_SHORT).show(),
                                (e) -> Snackbar.make(requireView(), R.string.delete_error, Snackbar.LENGTH_SHORT).show()
                        );
            });
            alertDialog.show();
            return true;
        } else if (preference == color) {
            var dialog = new ColorPickerDialogFragment();
            dialog.show(getChildFragmentManager(), null);
            dialog.setOnDismissListener(() -> name.setText(Preferences.getChat().getName()));
        }
        return false;
    }

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference preference) {
        if (preference == name) {
            var dialog = createNameDialog(requireContext());
            dialog.setOnDismissListener(d -> {
                name.setText(Preferences.getChat().getName());
            });
            dialog.show();
        } else if (preference == channel) {
            var dialog = createChannelDialog(requireContext());
            dialog.setOnDismissListener(d -> {
                channel.setText(Preferences.getChat().getChannel());
            });
            dialog.show();
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        if (preference == katex) {
            if (!(newValue instanceof Boolean)) return false;
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
            if (!(newValue instanceof Boolean)) return false;
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

    public static AlertDialog createNameDialog(@NonNull Context context) {
        @Value
        @EqualsAndHashCode(of = "value")
        class NameItem implements ViewUtils.ChipItem {
            CharSequence label;
            String value;

            public NameItem(@NonNull Context context, @NonNull String name) {
                this.value = name;
                this.label = MessageUtils.formatName(context, name, true);
            }
        }

        var suggestions = Preferences.getChat().getRecentNames().stream()
                .map(name -> new NameItem(context, name))
                .collect(Collectors.toList());
        Collections.reverse(suggestions);

        return ViewUtils.createPreferenceDialog(
                context,
                R.string.preferences_chat_name_title,
                () -> Preferences.getChat().getName(),
                (str) -> Preferences.getChat().setName(str),
                suggestions
        );
    }

    public static AlertDialog createChannelDialog(@NonNull Context context) {
        @Value
        @EqualsAndHashCode(of = "value")
        class ChannelItem implements ViewUtils.ChipItem {
            CharSequence label;
            String value;

            public ChannelItem(@NonNull Context context, @NonNull String name) {
                this.value = name;
                this.label = MessageUtils.formatChannel(context, name);
            }
        }

        var suggestions = Preferences.getChat().getRecentChannels().stream()
                .map(name -> new ChannelItem(context, name))
                .collect(Collectors.toList());
        Collections.reverse(suggestions);

        return ViewUtils.createPreferenceDialog(
                context,
                R.string.preferences_chat_channel_title,
                () -> Preferences.getChat().getChannel(),
                (str) -> Preferences.getChat().setChannel(str),
                suggestions
        );
    }
}
