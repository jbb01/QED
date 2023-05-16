package com.jonahbauer.qed.model.viewmodel;

import android.app.Application;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.*;

import com.jonahbauer.qed.BuildConfig;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.model.Album;
import com.jonahbauer.qed.model.room.AlbumDao;
import com.jonahbauer.qed.model.room.Database;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.networking.async.QEDPageReceiver;
import com.jonahbauer.qed.networking.pages.QEDGalleryPages;
import com.jonahbauer.qed.model.AlbumFilter;
import com.jonahbauer.qed.util.Preferences;
import com.jonahbauer.qed.util.StatusWrapper;

import java.time.Instant;
import java.util.Objects;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class AlbumViewModel extends InfoViewModel<Album> {
    private static final String LOG_TAG = AlbumViewModel.class.getName();
    private static final String SAVED_FILER = "album_filter";
    private static final String SAVED_EXPANDED = "album_filter_expanded";

    private final AlbumDao mAlbumDao;

    private final MutableLiveData<AlbumFilter> mFilter;
    private final MutableLiveData<Boolean> mOffline = new MutableLiveData<>(false);

    private final CompositeDisposable mDisposable = new CompositeDisposable();

    // saved view state
    private final MutableLiveData<Boolean> mExpanded;

    public AlbumViewModel(@NonNull Application application, @NonNull SavedStateHandle savedStateHandle) {
        super(application);
        mAlbumDao = Database.getInstance(application).albumDao();
        mFilter = savedStateHandle.getLiveData(SAVED_FILER, AlbumFilter.EMPTY);
        mExpanded = savedStateHandle.getLiveData(SAVED_EXPANDED, false);
    }

    public void load(@NonNull Album album) {
        load(album, getFilterValue());
    }

    public void filter(@NonNull AlbumFilter filter) {
        load(getAlbumValue(), filter);
    }

    public void load(@NonNull Album album, @NonNull AlbumFilter filter) {
        load(album, filter, Preferences.getGallery().isOfflineMode());
    }

    public void load(@NonNull Album album, @NonNull AlbumFilter filter, boolean offline) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "Loading album " + album + " with filters " + filter + ".");
        }

        mDisposable.clear();
        mFilter.setValue(filter);
        mOffline.setValue(offline);
        submit(StatusWrapper.preloaded(album));

        reload();
    }

    private void reload() {
        if (isOffline()) {
            loadDatabase();
        } else {
            loadInternet();
        }
    }

    @Override
    protected @NonNull CharSequence getTitle(@NonNull Album album) {
        return album.getName();
    }

    @Override
    protected @StringRes Integer getDefaultTitle() {
        return R.string.title_fragment_album;
    }

    public LiveData<AlbumFilter> getFilter() {
        return mFilter;
    }

    public LiveData<Boolean> getOffline() {
        return mOffline;
    }

    public MutableLiveData<Boolean> getExpanded() {
        return mExpanded;
    }

    private void loadInternet() {
        var album = getAlbumValue();
        var filter = getFilterValue();

        Callback callback = new Callback(!filter.isEmpty());
        mDisposable.add(
                QEDGalleryPages.getAlbum(album, filter, callback)
        );
    }

    private void loadDatabase() {
        var album = getAlbumValue();

        var observable = Observable.combineLatest(
                mAlbumDao.findById(album.getId()).toObservable(),
                mAlbumDao.findImagesByAlbum(album.getId()).toObservable(),
                Pair::create
        );

        mDisposable.add(
                observable.subscribeOn(Schedulers.io())
                          .observeOn(AndroidSchedulers.mainThread())
                          .subscribe(
                                  pair -> {
                                      var out = pair.first;
                                      out.getImages().clear();
                                      out.getImages().addAll(pair.second);

                                      if (out.getImages().size() > 0) {
                                          submit(StatusWrapper.loaded(out));
                                      } else {
                                          submit(StatusWrapper.error(out, Reason.EMPTY));
                                      }
                                  },
                                  e -> submit(StatusWrapper.error(album, e))
                          )
        );
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mDisposable.clear();
    }

    public @NonNull Album getAlbumValue() {
        var wrapper = getValueStatus().getValue();
        if (wrapper == null) return new Album(Album.NO_ID);
        var album = wrapper.getValue();
        if (album == null) return new Album(Album.NO_ID);
        return album;
    }

    public @NonNull AlbumFilter getFilterValue() {
        return Objects.requireNonNullElse(mFilter.getValue(), AlbumFilter.EMPTY);
    }

    private boolean isOffline() {
        return Objects.requireNonNullElse(mOffline.getValue(), false);
    }

    private class Callback implements QEDPageReceiver<Album> {
        private final boolean mFiltered;

        private Callback(boolean filtered) {
            this.mFiltered = filtered;
        }

        @Override
        public void onResult(@NonNull Album out) {
            var images = out.getImages();
            if (images.size() > 0)  {
                submit(StatusWrapper.loaded(out));
            } else {
                submit(StatusWrapper.error(out, Reason.EMPTY));
            }

            if (!mFiltered) {
                out.setLoaded(Instant.now());

                //noinspection ResultOfMethodCallIgnored
                mAlbumDao.insertOrUpdateAlbum(out)
                         .subscribeOn(Schedulers.io())
                         .subscribe(
                                 () -> {},
                                 e -> Log.e(LOG_TAG, "Error inserting album into database.", e)
                         );
            }

            //noinspection ResultOfMethodCallIgnored
            mAlbumDao.insertImages(out.getImages())
                     .subscribeOn(Schedulers.io())
                     .subscribe(() -> {}, e -> Log.e(LOG_TAG, "Error inserting images into database.", e));
        }

        @Override
        public void onError(Album out, @NonNull Reason reason, @Nullable Throwable cause) {
            QEDPageReceiver.super.onError(out, reason, cause);

            if (reason == Reason.NETWORK && out != null) {
                load(out, AlbumFilter.EMPTY, true);
            } else {
                submit(StatusWrapper.error(out, reason));
            }
        }
    }
}
