package com.jonahbauer.qed.model.viewmodel;

import static com.jonahbauer.qed.util.StatusWrapper.STATUS_LOADED;
import static com.jonahbauer.qed.util.StatusWrapper.STATUS_PRELOADED;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.jonahbauer.qed.model.Album;
import com.jonahbauer.qed.model.Image;
import com.jonahbauer.qed.model.room.AlbumDao;
import com.jonahbauer.qed.model.room.Database;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.networking.async.QEDPageReceiver;
import com.jonahbauer.qed.networking.pages.QEDGalleryPages;
import com.jonahbauer.qed.networking.pages.QEDGalleryPages.Filter;
import com.jonahbauer.qed.util.Preferences;
import com.jonahbauer.qed.util.StatusWrapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class AlbumViewModel extends AndroidViewModel implements QEDPageReceiver<Album> {
    private static final String LOG_TAG = AlbumViewModel.class.getName();

    private final AlbumDao mAlbumDao;

    private final MutableLiveData<Boolean> mOffline = new MutableLiveData<>(false);
    private final MutableLiveData<StatusWrapper<Pair<Album, List<Image>>>> mFilteredAlbum = new MutableLiveData<>();
    private final MutableLiveData<StatusWrapper<Album>> mAlbum = new MutableLiveData<>();

    private final CompositeDisposable mDisposable = new CompositeDisposable();

    public AlbumViewModel(@NonNull Application application) {
        super(application);
        mAlbumDao = Database.getInstance(application).albumDao();
        mFilteredAlbum.observeForever(wrapper -> {
            mAlbum.setValue(StatusWrapper.wrap(wrapper.getValue().first, wrapper.getCode(), wrapper.getReason()));
        });
    }

    /**
     * Initialize this view model to use the given {@link Album}.
     */
    public void init(@NonNull Album album) {
        var albumWrapper = mFilteredAlbum.getValue();
        if (albumWrapper != null) {
            var pair = albumWrapper.getValue();
            if (pair != null && pair.first != null && pair.first.getId() == album.getId()) {
                return;
            }
        }

        this.mFilteredAlbum.setValue(StatusWrapper.wrap(Pair.create(album, Collections.emptyList()), STATUS_PRELOADED));
    }

    /**
     * Loads the album from internet or database when offline mode is activated or an IO error
     * occurred during loading from website.
     * @throws IllegalStateException when {@link #init(Album)} has not been called yet.
     */
    public void load() {
        var albumWrapper = mFilteredAlbum.getValue();
        if (albumWrapper == null) throw new IllegalStateException();

        var pair = albumWrapper.getValue();
        if (pair == null) throw new IllegalStateException();
        var album = pair.first;
        if (album == null) throw new IllegalStateException();

        this.mFilteredAlbum.setValue(StatusWrapper.wrap(Pair.create(album, Collections.emptyList()), STATUS_PRELOADED));

        if (Preferences.gallery().isOfflineMode()) {
            mOffline.setValue(true);
            loadFromDatabase(album);
        } else {
            mOffline.setValue(false);
            loadFromInternet(album);
        }
    }

    /**
     * Applies the specified filters to the image list. The album is left untouched.
     * @throws IllegalStateException when in offline mode or {@link #load()} has not yet been called
     */
    public void filter(@Nullable Map<Filter, String> filters) {
        var albumWrapper = mFilteredAlbum.getValue();
        if (albumWrapper == null) throw new IllegalStateException();

        var pair = albumWrapper.getValue();
        if (pair == null) throw new IllegalStateException();

        var album = pair.first;
        if (album == null || !album.isLoaded()) throw new IllegalStateException();

        this.mFilteredAlbum.setValue(StatusWrapper.wrap(Pair.create(album, Collections.emptyList()), STATUS_PRELOADED));

        var receiver = new QEDPageReceiver<Album>() {
            @Override
            public void onResult(@NonNull Album out) {
                if (out.getImages().size() > 0) {
                    AlbumViewModel.this.mFilteredAlbum.setValue(StatusWrapper.wrap(Pair.create(album, out.getImages()), STATUS_LOADED));
                } else {
                    AlbumViewModel.this.mFilteredAlbum.setValue(StatusWrapper.wrap(Pair.create(album, Collections.emptyList()), Reason.EMPTY));
                }
            }

            @Override
            public void onError(Album out, @NonNull Reason reason, @Nullable Throwable cause) {
                QEDPageReceiver.super.onError(out, reason, cause);
                AlbumViewModel.this.mFilteredAlbum.setValue(StatusWrapper.wrap(Pair.create(album, Collections.emptyList()), reason));
            }
        };

        mDisposable.add(
                QEDGalleryPages.getAlbum(album, filters, receiver)
        );
    }

    public void loadFromInternet(@NonNull Album album) {
        if (!album.isLoaded()) {
            mDisposable.add(
                    QEDGalleryPages.getAlbum(album, null, this)
            );
        } else {
            onResult(album);
        }
    }

    /**
     * Loads the album from the local database.
     */
    public void loadFromDatabase(@NonNull Album album) {
        AtomicBoolean done = new AtomicBoolean(false);
        Runnable finish = () -> {
            if (done.getAndSet(true)) {
                if (album.getImages().size() > 0) {
                    AlbumViewModel.this.mFilteredAlbum.setValue(StatusWrapper.wrap(Pair.create(album, album.getImages()), STATUS_LOADED));
                } else {
                    AlbumViewModel.this.mFilteredAlbum.setValue(StatusWrapper.wrap(Pair.create(album, Collections.emptyList()), Reason.EMPTY));
                }
            }
        };

        mDisposable.addAll(
            mAlbumDao.findById(album.getId())
                     .subscribeOn(Schedulers.io())
                     .observeOn(AndroidSchedulers.mainThread())
                     .subscribe(
                             (album0) -> {
                                 album.setName(album0.getName());
                                 album.setOwner(album0.getOwner());
                                 album.setCreationDate(album0.getCreationDate());
                                 album.setPersons(album0.getPersons());
                                 album.setCategories(album0.getCategories());
                                 album.setDates(album0.getDates());
                                 album.setPrivate_(album0.isPrivate_());
                                 album.setImageListDownloaded(album0.isImageListDownloaded());

                                 finish.run();
                             },
                             (e) -> {
                                 mFilteredAlbum.setValue(StatusWrapper.wrap(Pair.create(album, Collections.emptyList()), e));
                             }
                     ),
            mAlbumDao.findImagesByAlbum(album.getId())
                     .subscribeOn(Schedulers.io())
                     .observeOn(AndroidSchedulers.mainThread())
                     .subscribe(
                             (images) -> {
                                 album.getImages().clear();
                                 album.getImages().addAll(images);

                                 finish.run();
                             },
                             (e) -> {
                                 mFilteredAlbum.setValue(StatusWrapper.wrap(Pair.create(album, Collections.emptyList()), e));
                             }
                     )
        );
    }

    @Override
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void onResult(@NonNull Album out) {
        out.setImageListDownloaded(true);
        out.setLoaded(true);
        if (out.getImages().size() > 0) {
            AlbumViewModel.this.mFilteredAlbum.setValue(StatusWrapper.wrap(Pair.create(out, out.getImages()), STATUS_LOADED));
        } else {
            AlbumViewModel.this.mFilteredAlbum.setValue(StatusWrapper.wrap(Pair.create(out, Collections.emptyList()), Reason.EMPTY));
        }
        this.mOffline.setValue(false);

        this.mAlbumDao.insertOrUpdateAlbum(out)
                      .subscribeOn(Schedulers.io())
                      .subscribe(() -> {}, e -> Log.e(LOG_TAG, "Error inserting album into database.", e));

        this.mAlbumDao.insertImages(out.getImages())
                      .subscribeOn(Schedulers.io())
                      .subscribe(() -> {}, e -> Log.e(LOG_TAG, "Error inserting images into database.", e));
    }

    @Override
    public void onError(Album out, @NonNull Reason reason, @Nullable Throwable cause) {
        QEDPageReceiver.super.onError(out, reason, cause);

        if (Reason.NETWORK.equals(reason)) {
            mOffline.setValue(true);
            loadFromDatabase(out);
        } else {
            this.mFilteredAlbum.setValue(StatusWrapper.wrap(Pair.create(out, Collections.emptyList()), reason));
        }
    }

    public LiveData<StatusWrapper<Pair<Album,List<Image>>>> getFilteredAlbum() {
        return mFilteredAlbum;
    }

    public LiveData<StatusWrapper<Album>> getAlbum() {
        return mAlbum;
    }

    public LiveData<Boolean> getOffline() {
        return mOffline;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mDisposable.clear();
    }
}