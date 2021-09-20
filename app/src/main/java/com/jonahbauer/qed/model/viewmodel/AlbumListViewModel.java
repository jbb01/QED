package com.jonahbauer.qed.model.viewmodel;

import static com.jonahbauer.qed.util.StatusWrapper.STATUS_LOADED;
import static com.jonahbauer.qed.util.StatusWrapper.STATUS_PRELOADED;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.jonahbauer.qed.model.Album;
import com.jonahbauer.qed.model.room.AlbumDao;
import com.jonahbauer.qed.model.room.Database;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.networking.async.QEDPageReceiver;
import com.jonahbauer.qed.networking.pages.QEDGalleryPages;
import com.jonahbauer.qed.util.Preferences;
import com.jonahbauer.qed.util.StatusWrapper;

import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class AlbumListViewModel extends AndroidViewModel implements QEDPageReceiver<List<Album>> {
    private static final String LOG_TAG = AlbumListViewModel.class.getName();

    private final AlbumDao mAlbumDao;
    private final MutableLiveData<StatusWrapper<List<Album>>> mAlbums = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mOffline = new MutableLiveData<>(false);

    private final CompositeDisposable mDisposable = new CompositeDisposable();

    public AlbumListViewModel(@NonNull Application application) {
        super(application);

        mAlbumDao = Database.getInstance(application).albumDao();
        mAlbums.setValue(StatusWrapper.wrap(Collections.emptyList(), STATUS_LOADED));
    }

    public void load() {
        if (Preferences.gallery().isOfflineMode()) {
            mOffline.setValue(true);
            loadFromDatabase();
        } else {
            mOffline.setValue(false);
            loadFromInternet();
        }
    }

    private void loadFromInternet() {
        this.mAlbums.setValue(StatusWrapper.wrap(Collections.emptyList(), STATUS_PRELOADED));
        mDisposable.add(
                QEDGalleryPages.getAlbumList(this)
        );
    }

    private void loadFromDatabase() {
        mAlbums.setValue(StatusWrapper.wrap(Collections.emptyList(), STATUS_PRELOADED));
        mDisposable.add(
                mAlbumDao.getAll()
                         .subscribeOn(Schedulers.io())
                         .observeOn(AndroidSchedulers.mainThread())
                         .subscribe(
                                 albums -> {
                                     if (albums != null && albums.size() > 0) {
                                         mAlbums.setValue(StatusWrapper.wrap(albums, STATUS_LOADED));
                                     } else {
                                         mAlbums.setValue(StatusWrapper.wrap(Collections.emptyList(), Reason.EMPTY));
                                     }
                                 },
                                 e -> mAlbums.setValue(StatusWrapper.wrap(Collections.emptyList(), e))
                         )
        );
    }

    public LiveData<StatusWrapper<List<Album>>> getAlbums() {
        return mAlbums;
    }

    public LiveData<Boolean> getOffline() {
        return mOffline;
    }

    @Override
    public void onPageReceived(@NonNull List<Album> out) {
        if (out.size() > 0) {
            this.mAlbums.setValue(StatusWrapper.wrap(out, STATUS_LOADED));

            //noinspection ResultOfMethodCallIgnored
            this.mAlbumDao.insertAlbums(out)
                          .subscribeOn(Schedulers.io())
                          .subscribe(
                                  () -> {},
                                  e -> Log.e(LOG_TAG, "Error inserting album list into database.", e)
                          );
        } else {
            mAlbums.setValue(StatusWrapper.wrap(Collections.emptyList(), Reason.EMPTY));
        }
    }

    @Override
    public void onError(List<Album> out, @NonNull Reason reason, @Nullable Throwable cause) {
        QEDPageReceiver.super.onError(out, reason, cause);

        if (Reason.NETWORK.equals(reason)) {
            mOffline.setValue(true);
            loadFromDatabase();
        } else {
            this.mAlbums.setValue(StatusWrapper.wrap(out, reason));
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mDisposable.clear();
    }
}