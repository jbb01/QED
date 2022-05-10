package com.jonahbauer.qed.activities.settings;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import com.google.android.material.snackbar.Snackbar;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.model.room.Database;
import com.jonahbauer.qed.util.Actions;
import com.jonahbauer.qed.util.FileUtils;
import com.jonahbauer.qed.util.Preferences;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class GalleryPreferenceFragment extends AbstractPreferenceFragment implements PreferenceFragment, Preference.OnPreferenceClickListener {
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
    public boolean onPreferenceClick(@NonNull Preference preference) {
        Context context = getActivity();
        if (context == null) return false;

        if (preference == deleteImages) {
            showDeleteDialog(R.string.preferences_gallery_confirm_delete_images, this::deleteImages);
            return true;
        } else if (preference == deleteThumbnails) {
            showDeleteDialog(R.string.preferences_gallery_confirm_delete_thumbnails, this::deleteThumbnails);
            return true;
        } else if (preference == showDir) {
            var dir = context.getExternalFilesDir(getString(R.string.gallery_folder_images));
            if (dir == null) return false;

            if (!Actions.openContent(context, Uri.parse(dir.toString()), "resource/folder")) {
                notify(context, getView(), R.string.no_file_explorer);
            }

            return true;
        } else if (preference == deleteDatabase) {
            showDeleteDialog(R.string.preferences_gallery_confirm_delete_gallery_database, this::deleteDatabase);
            return true;
        }
        return false;
    }

    private void showDeleteDialog(@StringRes int message, Runnable deleteRunnable) {
        var context = requireContext();
        var builder = new AlertDialog.Builder(context);
        builder.setMessage(message);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.delete, (dialog, which) -> {
            deleteRunnable.run();
        });
        builder.show();
    }

    private void notify(Context context, View view, @StringRes int message) {
        if (view != null) {
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void deleteDatabase() {
        var completable = Completable.mergeArrayDelayError(
                Database.getInstance(requireContext()).albumDao().clearImages(),
                Database.getInstance(requireContext()).albumDao().clearAlbums()
        );

        var context = requireContext().getApplicationContext();
        completable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> notify(context, getView(), R.string.deleted),
                        (e) -> notify(context, getView(), R.string.delete_error)
                );
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void deleteThumbnails() {
        var context = requireContext().getApplicationContext();
        Database.getInstance(requireContext()).albumDao().clearThumbnails()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> notify(context, getView(), R.string.deleted),
                        (e) -> notify(context, getView(), R.string.delete_error)
                );
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void deleteImages() {
        var context = requireContext().getApplicationContext();
        var dir = context.getExternalFilesDir(getString(R.string.gallery_folder_images));

        var completable = Completable.fromAction(() -> {
            FileUtils.cleanDirectory(dir);
        });
        completable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> notify(context, getView(), R.string.deleted),
                        (e) -> notify(context, getView(), R.string.delete_error)
                );
    }

    @Override
    public int getTitle() {
        return R.string.preferences_header_gallery;
    }
}
