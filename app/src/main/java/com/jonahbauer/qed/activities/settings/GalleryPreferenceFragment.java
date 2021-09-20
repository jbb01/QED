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

import com.google.android.material.snackbar.Snackbar;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.model.room.Database;
import com.jonahbauer.qed.util.Preferences;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class GalleryPreferenceFragment extends PreferenceFragmentCompat implements PreferenceFragment, Preference.OnPreferenceClickListener {
    private Preference deleteThumbnails;
    private Preference deleteImages;
    private Preference deleteDatabase;
    private Preference showDir;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_gallery, rootKey);

        deleteThumbnails = findPreference(Preferences.gallery().keys().deleteThumbnails());
        assert deleteThumbnails != null;
        deleteThumbnails.setOnPreferenceClickListener(this);

        deleteImages = findPreference(Preferences.gallery().keys().deleteImages());
        assert deleteImages != null;
        deleteImages.setOnPreferenceClickListener(this);

        showDir = findPreference(Preferences.gallery().keys().showDir());
        assert showDir != null;
        showDir.setOnPreferenceClickListener(this);

        deleteDatabase = findPreference(Preferences.gallery().keys().deleteDatabase());
        assert deleteDatabase != null;
        deleteDatabase.setOnPreferenceClickListener(this);
    }

    @Override
    @SuppressWarnings("ResultOfMethodCallIgnored")
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
                Database.getInstance(requireContext()).albumDao().clearThumbnails()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> Snackbar.make(requireView(), R.string.deleted, Snackbar.LENGTH_SHORT).show(),
                                (e) -> Snackbar.make(requireView(), R.string.delete_error, Snackbar.LENGTH_SHORT).show()
                        );
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
                AtomicBoolean done = new AtomicBoolean(false);
                AtomicBoolean error = new AtomicBoolean(false);

                Action action = () -> {
                    if (done.getAndSet(true)) {
                        Snackbar.make(requireView(), R.string.deleted, Snackbar.LENGTH_SHORT).show();
                    }
                };

                Consumer<Throwable> errorHandler = (e) -> {
                    if (!error.getAndSet(true)) {
                        Snackbar.make(requireView(), R.string.delete_error, Snackbar.LENGTH_SHORT).show();
                    }
                };

                Database.getInstance(requireContext()).albumDao().clearImages()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(action, errorHandler);

                Database.getInstance(requireContext()).albumDao().clearAlbums()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(action, errorHandler);
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
