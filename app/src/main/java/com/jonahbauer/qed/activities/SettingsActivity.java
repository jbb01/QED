package com.jonahbauer.qed.activities;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceFragmentCompat;

import com.jonahbauer.qed.R;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SettingsActivity extends AppCompatPreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private List<Header> mHeaders;
    SharedPreferences sharedPreferences;

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = (preference, value) -> {
        String stringValue = value.toString();

        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            int index = listPreference.findIndexOfValue(stringValue);

            preference.setSummary(
                    index >= 0
                            ? listPreference.getEntries()[index]
                            : null);

        } else {
            preference.setSummary(stringValue);
        }
        return true;
    };

    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    private void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, sharedPreferences.getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getListView().setDivider(new ColorDrawable(Color.parseColor("#FFDDDDDD")));
        getListView().setDividerHeight(2);
        setupActionBar();

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        sharedPreferences = getSharedPreferences(getString(R.string.preferences_shared_preferences), MODE_PRIVATE);
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    @Override
    public void setListAdapter(ListAdapter adapter) {
        int i, count;

        if (mHeaders == null) {
            mHeaders = new ArrayList<>();
            // When the saved state provides the list of headers, onBuildHeaders is not called
            // so we build it from the adapter given, then use our own adapter

            count = adapter.getCount();
            for (i = 0; i < count; ++i) {
                mHeaders.add((Header) adapter.getItem(i));
            }
        }

        super.setListAdapter(new HeaderAdapter(this, mHeaders, R.layout.preference_header_item, true));
    }

    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragmentCompat.class.getName().equals(fragmentName)
                || ChatPreferenceFragment.class.getName().equals(fragmentName)
                || GalleryPreferenceFragment.class.getName().equals(fragmentName);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Map<String, ?> all = sharedPreferences.getAll();
        if (all.get(key) instanceof String) {
            this.sharedPreferences.edit().putString(key,sharedPreferences.getString(key,"")).apply();
        } else if (all.get(key) instanceof Boolean) {
            this.sharedPreferences.edit().putBoolean(key,sharedPreferences.getBoolean(key,false)).apply();
        } else if (all.get(key) instanceof Integer) {
            this.sharedPreferences.edit().putInt(key,sharedPreferences.getInt(key,0)).apply();
        } else if (all.get(key) instanceof Float) {
            this.sharedPreferences.edit().putFloat(key,sharedPreferences.getFloat(key,0f)).apply();
        } else if (all.get(key) instanceof Long) {
            this.sharedPreferences.edit().putLong(key,sharedPreferences.getLong(key,0L)).apply();
        } else if (all.get(key) instanceof Set<?>) {
            this.sharedPreferences.edit().putStringSet(key,sharedPreferences.getStringSet(key,null)).apply();
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class ChatPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_chat);
            setHasOptionsMenu(true);

            ((SettingsActivity) getActivity()).bindPreferenceSummaryToValue(findPreference(getString(R.string.preferences_name_key)));
            ((SettingsActivity) getActivity()).bindPreferenceSummaryToValue(findPreference(getString(R.string.preferences_channel_key)));
        }

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {}

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GalleryPreferenceFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {
        private Preference deleteThumbnails;
        private Preference deleteImages;
        private Preference showDir;
//        private SwitchPreference savePublic;
        private boolean permissionGranted;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_gallery);
            setHasOptionsMenu(true);

            deleteThumbnails = findPreference(getString(R.string.preferences_gallery_delete_thumbnails_key));
            deleteImages = findPreference(getString(R.string.preferences_gallery_delete_images_key));
            showDir = findPreference(getString(R.string.preferences_gallery_show_dir_key));
//            savePublic = (SwitchPreference) findPreference(getString(R.string.preferences_gallery_save_public_key));
            deleteThumbnails.setOnPreferenceClickListener(this);
            deleteImages.setOnPreferenceClickListener(this);
            showDir.setOnPreferenceClickListener(this);
//            savePublic.setOnPreferenceChangeListener(this);
        }

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {}

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            if (preference.equals(deleteImages)) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
                alertDialog.setMessage(R.string.confirm_delete_images);
                alertDialog.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                alertDialog.setPositiveButton(R.string.delete, (dialog, which) -> {
                    File dir = getContext().getExternalFilesDir(getContext().getString(R.string.gallery_folder_images));
                    if (dir != null && dir.exists()) {
                        try {
                            FileUtils.cleanDirectory(dir);
                        } catch (IOException ignored) {
                        }
                    }
                });
                alertDialog.show();
                return true;
            } else if (preference.equals(deleteThumbnails)) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
                alertDialog.setMessage(R.string.confirm_delete_thumbnails);
                alertDialog.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                alertDialog.setPositiveButton(R.string.delete, (dialog, which) -> {
                    File dir = getContext().getExternalFilesDir(getContext().getString(R.string.gallery_folder_thumbnails));
                    if (dir != null && dir.exists()) {
                        try {
                            FileUtils.cleanDirectory(dir);
                        } catch (IOException ignored) {
                        }
                    }
                });
                alertDialog.show();
                return true;
            } else if (preference.equals(showDir)) {
                Context context = getContext();
                if (context == null) return false;
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
            }
            return false;
        }

//        @Override
//        public boolean onPreferenceChange(Preference preference, Object o) {
//            if (preference.equals(savePublic)) {
//                boolean newState = (Boolean) o;
//                if (!newState) return true;
//                if (permissionGranted) {
//                    permissionGranted = false;
//                    return true;
//                }
//
//                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 31415);
//                    return false;
//                } else return true;
//            }
//            return true;
//        }

//        @Override
//        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//            switch (requestCode) {
//                case 31415: {
//                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                        permissionGranted = true;
//                        savePublic.setChecked(true);
//                    }
//                }
//            }
//        }
    }

    private static class HeaderAdapter extends ArrayAdapter<Header> {
        private static class HeaderViewHolder {
            ImageView icon;
            TextView title;
            TextView summary;
        }

        private LayoutInflater mInflater;
        private int mLayoutResId;
        private boolean mRemoveIconIfEmpty;

        HeaderAdapter(Context context, List<Header> objects, int layoutResId,
                      boolean removeIconBehavior) {
            super(context, 0, objects);
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mLayoutResId = layoutResId;
            mRemoveIconIfEmpty = removeIconBehavior;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            HeaderViewHolder holder;
            View view;

            if (convertView == null) {
                view = mInflater.inflate(mLayoutResId, parent, false);
                holder = new HeaderViewHolder();
                holder.icon = view.findViewById(R.id.icon);
                holder.title = view.findViewById(R.id.title);
                holder.summary =  view.findViewById(R.id.summary);
                view.setTag(holder);
            } else {
                view = convertView;
                holder = (HeaderViewHolder) view.getTag();
            }

            // All view fields must be updated every time, because the view may be recycled
            Header header = getItem(position);
            if (mRemoveIconIfEmpty) {
                assert header != null;
                if (header.iconRes == 0) {
                    holder.icon.setVisibility(View.GONE);
                } else {
                    holder.icon.setVisibility(View.VISIBLE);
                    holder.icon.setImageResource(header.iconRes);
                }
            } else {
                assert header != null;
                holder.icon.setImageResource(header.iconRes);
            }
            holder.title.setText(header.getTitle(getContext().getResources()));
            CharSequence summary = header.getSummary(getContext().getResources());
            if (!TextUtils.isEmpty(summary)) {
                holder.summary.setVisibility(View.VISIBLE);
                holder.summary.setText(summary);
            } else {
                holder.summary.setVisibility(View.GONE);
            }

            return view;
        }
    }
}
