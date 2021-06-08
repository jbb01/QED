package com.jonahbauer.qed.activities.settings;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.database.GalleryDatabase;
import com.jonahbauer.qed.util.Preferences;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class GalleryPreferenceFragment extends PreferenceFragmentCompat implements PreferenceFragment, Preference.OnPreferenceClickListener {
    private Preference deleteThumbnails;
    private Preference deleteImages;
    private Preference deleteDatabase;
    private Preference showDir;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_gallery, rootKey);

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

    @Override
    public int getTitle() {
        return R.string.preferences_header_gallery;
    }
}
