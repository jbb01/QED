package eu.jonahbauer.qed.util;

import android.content.Context;
import android.text.Html;
import android.util.Log;
import androidx.annotation.*;
import androidx.appcompat.app.AlertDialog;
import eu.jonahbauer.qed.BuildConfig;
import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.model.Release;
import eu.jonahbauer.qed.networking.pages.GitHubAPI;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.json.JSONException;
import org.jsoup.HttpStatusException;

import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.Callable;

public class UpdateChecker implements Callable<Release> {
    private static final String LOG_TAG = UpdateChecker.class.getName();
    private static final Object LOCK = new Object();

    /**
     * Asynchronously queries the GitHub-API for the latest prerelease or release (depending on preferences).
     * @return the latest (pre)release version if it is more recent than the current version and not
     * {@linkplain #suppressUpdateNotifications(Release.Version) ignored}.
     */
    public static @NonNull Maybe<Release> getUpdateRelease() {
        var updateChecker = new UpdateChecker();
        return Maybe.fromCallable(updateChecker)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(t -> {
                    // ignore network errors
                    if (t instanceof IOException && !(t instanceof HttpStatusException)) return;

                    Log.e(LOG_TAG, "Could not check for updates.", t);
                })
                .onErrorComplete();
    }

    @MainThread
    public static void showUpdateDialog(@UiContext @NonNull Context context, @NonNull Release release) {
        var builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.update_dialog_title);
        builder.setMessage(Html.fromHtml(context.getString(
                release.isPrerelease()
                        ? R.string.update_dialog_message_prerelease
                        : R.string.update_dialog_message,
                release.getVersion()
        ), Html.FROM_HTML_MODE_LEGACY));
        builder.setPositiveButton(R.string.update_dialog_show, (d, which) -> {
            Actions.open(context, release.getUrl());
        });
        builder.setNegativeButton(R.string.update_dialog_ignore, (d, which) -> {
            suppressUpdateNotifications(release.getVersion());
        });
        builder.setNeutralButton(R.string.update_dialog_remind_later, (d, which) -> {});
        builder.show();
    }

    /**
     * Adds the given version to the list of ignored releases. There will be no more update notifications for this
     * release version.
     * @param version a release version
     */
    public static void suppressUpdateNotifications(@NonNull Release.Version version) {
        Completable.fromAction(() -> {
            synchronized (LOCK) {
                var set = Preferences.getGeneral().getUpdateCheckDontSuggest();
                set = set == null ? new HashSet<>() : new HashSet<>(set);
                var changed = set.add(version);
                if (changed) {
                    Preferences.getGeneral().setUpdateCheckDontSuggest(set);
                }
            }
        }).subscribeOn(Schedulers.io()).subscribe();
    }

    /**
     * Queries the GitHub-API for the latest prerelease or release (depending on preferences).
     * @return the latest (pre)release version if it is more recent than the current version and not
     * {@linkplain #suppressUpdateNotifications(Release.Version) ignored}.
     */
    @Override
    @WorkerThread
    public @Nullable Release call() throws Exception {
        if (!BuildConfig.UPDATE_CHECK) return null;

        var enabled = Preferences.getGeneral().isUpdateCheckEnabled();
        if (!enabled) return null;

        var includesPrereleases = Preferences.getGeneral().isUpdateCheckIncludesPrereleases();
        var latestRelease = getLatestRelease(includesPrereleases);
        if (Release.getCurrentRelease().compareTo(latestRelease) >= 0) return null;

        var ignored = Preferences.getGeneral().getUpdateCheckDontSuggest();
        if (ignored != null && ignored.contains(latestRelease.getVersion())) {
            return null;
        } else {
            return latestRelease;
        }
    }

    /**
     * Queries the GitHub API to get the latest release.
     * @param includePrereleases whether to include prereleases
     * @return the latest non-prerelease release when {@code includePrereleases} is set, the latest release otherwise
     * @throws JSONException if the GitHub API Response could not be parsed
     * @throws IOException if an I/O error occurrs
     */
    @WorkerThread
    public @NonNull Release getLatestRelease(boolean includePrereleases) throws JSONException, IOException {
        if (includePrereleases) {
            var releases = GitHubAPI.getAllReleases(1, 0);
            return releases.get(0);
        } else {
            return GitHubAPI.getLatestRelease();
        }
    }
}
