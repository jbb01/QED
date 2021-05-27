package com.jonahbauer.qed.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.jonahbauer.qed.BuildConfig;
import com.jonahbauer.qed.Pref;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.database.ChatDatabase;
import com.jonahbauer.qed.database.GalleryDatabase;
import com.jonahbauer.qed.layoutStuff.SeekBarPreference;
import com.jonahbauer.qed.pingNotifications.PingNotifications;
import com.jonahbauer.qed.util.Preferences;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class SettingsActivity extends AppCompatActivity implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback  {
    private static final String SAVED_INSTANCE_CONTENT_KEY = "content";
    private static final String SAVED_INSTANCE_CONTENT_KEY_KEY = "contentKey";

    private int mHeaderId;
    private int mContentId;

    private int mOrientation;

    private String mContent;
    private String mContentKey;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mOrientation = getResources().getConfiguration().orientation;

        if (mOrientation == Configuration.ORIENTATION_PORTRAIT) {
            mHeaderId = R.id.settings;
            mContentId = R.id.settings;
        } else if (mOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            mHeaderId = R.id.settings_header;
            mContentId = R.id.settings_content;
        }

        PreferenceFragmentCompat header = new SettingsFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(mHeaderId, header)
                .commit();

        if (savedInstanceState != null) {
            mContent = savedInstanceState.getString(SAVED_INSTANCE_CONTENT_KEY);
            mContentKey = savedInstanceState.getString(SAVED_INSTANCE_CONTENT_KEY_KEY);
        }

        if (mContent == null && mOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            mContent = GeneralPreferenceFragment.class.getName();
            mContentKey = getString(R.string.preferences_header_general);
        }

        if (mContent != null) {
            final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                    getClassLoader(),
                    mContent);

            // Replace the existing Fragment with the new Fragment
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction()
                    .replace(mContentId, fragment);

            if (mOrientation != Configuration.ORIENTATION_LANDSCAPE)
                transaction.addToBackStack(mContentKey);

            transaction.commit();
        }

    }

    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, @NonNull Preference pref) {
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                pref.getFragment());
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);

        mContent = pref.getFragment();
        mContentKey = pref.getKey();


        // Replace the existing Fragment with the new Fragment
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction()
                .replace(mContentId, fragment);

        if (mOrientation != Configuration.ORIENTATION_LANDSCAPE)
            transaction.addToBackStack(pref.getKey());

        transaction.commit();

        return true;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(SAVED_INSTANCE_CONTENT_KEY, mContent);
        outState.putString(SAVED_INSTANCE_CONTENT_KEY_KEY, mContentKey);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref, rootKey);
        }
    }

    public static class GeneralPreferenceFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener {
        private SwitchPreference pingNotifications;
        private Preference bugReport;
        private Preference github;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_general, rootKey);

            bugReport = findPreference(Preferences.general().keys().bugReport());
            bugReport.setOnPreferenceClickListener(this);

            github = findPreference(Preferences.general().keys().github());
            github.setOnPreferenceClickListener(this);

            if (BuildConfig.USES_FCM) {
                pingNotifications = findPreference(Pref.FCM.PING_NOTIFICATION);
                if (pingNotifications != null)
                    pingNotifications.setOnPreferenceClickListener(this);

                EditTextPreference pushPingServer = findPreference(Pref.FCM.PING_NOTIFICATION_SERVER);
                if (pushPingServer != null) {
                    if (BuildConfig.DEBUG) {
                        pushPingServer.setOnPreferenceClickListener(this);
                    } else {
                        pushPingServer.setEnabled(false);
                        pushPingServer.setVisible(false);
                    }
                }
            }
        }

        @Override
        public boolean onPreferenceClick(@NonNull Preference preference) {
            if (preference == pingNotifications) {
                if (pingNotifications.isChecked()) {
                    PingNotifications.getInstance(getContext()).registrationStage0();
                }
                return true;
            } else if (preference == bugReport) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_project_issue_tracker)));
                startActivity(intent);
                return true;
            } else if (preference == github) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_project)));
                startActivity(intent);
                return true;
            }

            return false;
        }
    }

    public static class ChatPreferenceFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {
        private Preference deleteDatabase;
        private SwitchPreference katex;
        private SwitchPreference links;

        public static final int[] maxShownRowsValues = {10_000, 20_000, 50_000, 100_000, 200_000, 500_000, Integer.MAX_VALUE};
        private static final String[] maxShownRowsStringValues = {"10.000", "20.000", "50.000", "100.000", "200.000", "500.000", "\u221E"};

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_chat, rootKey);
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
                startActivity(new Intent(getActivity(), SettingsActivity.class));
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
    }

    public static class GalleryPreferenceFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener {
        private Preference deleteThumbnails;
        private Preference deleteImages;
        private Preference deleteDatabase;
        private Preference showDir;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_gallery, rootKey);

            deleteThumbnails = findPreference(Preferences.gallery().keys().deleteThumbnails());
            deleteThumbnails.setOnPreferenceClickListener(this);

            deleteImages = findPreference(Preferences.gallery().keys().deleteImages());
            deleteImages.setOnPreferenceClickListener(this);

            showDir = findPreference(Preferences.gallery().keys().showDir());
            showDir.setOnPreferenceClickListener(this);

            deleteDatabase = findPreference(Preferences.gallery().keys().deleteDatabase());
            deleteDatabase.setOnPreferenceClickListener(this);
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            Context context = getActivity();
            if (context == null) return false;

            if (preference == deleteImages) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
                alertDialog.setMessage(R.string.preferences_gallery_confirm_delete_images);
                alertDialog.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                alertDialog.setPositiveButton(R.string.delete, (dialog, which) -> {
                    File dir = context.getExternalFilesDir(getString(R.string.gallery_folder_images));
                    if (dir != null && dir.exists()) {
                        try {
                            FileUtils.cleanDirectory(dir);
                        } catch (IOException ignored) {
                        }
                    }
                });
                alertDialog.show();
                return true;
            } else if (preference == deleteThumbnails) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
                alertDialog.setMessage(R.string.preferences_gallery_confirm_delete_thumbnails);
                alertDialog.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                alertDialog.setPositiveButton(R.string.delete, (dialog, which) -> {
                    try (GalleryDatabase galleryDatabase = new GalleryDatabase()) {
                        galleryDatabase.init(requireContext());
                        galleryDatabase.clearThumbnails();
                    }
                });
                alertDialog.show();
                return true;
            } else if (preference == showDir) {
                File dir = context.getExternalFilesDir("gallery");
                if (dir == null) return false;


                Intent intent2 = new Intent();
                intent2.setAction(Intent.ACTION_VIEW);
                intent2.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                Uri uri = Uri.parse(dir.toString());

                intent2.setDataAndType(uri, "resource/folder");
                List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(intent2, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }

                if (resInfoList.size() == 0) {
                    Toast.makeText(context, context.getString(R.string.no_file_explorer), Toast.LENGTH_SHORT).show();
                } else {
                    startActivity(intent2);
                }

                return true;
            } else if (preference == deleteDatabase) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
                alertDialog.setMessage(R.string.preferences_gallery_confirm_delete_gallery_database);
                alertDialog.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                alertDialog.setPositiveButton(R.string.delete, (dialog, which) -> {
                    GalleryDatabase db = new GalleryDatabase();
                    db.init(getContext());
                    db.clear();
                    db.close();
                });
                alertDialog.show();
                return true;
            }
            return false;
        }
    }
}
