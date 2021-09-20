package com.jonahbauer.qed.model.viewmodel;

import static com.jonahbauer.qed.util.StatusWrapper.STATUS_ERROR;
import static com.jonahbauer.qed.util.StatusWrapper.STATUS_LOADED;
import static com.jonahbauer.qed.util.StatusWrapper.STATUS_PRELOADED;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.jonahbauer.qed.model.Image;
import com.jonahbauer.qed.model.room.AlbumDao;
import com.jonahbauer.qed.model.room.Database;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.networking.async.QEDPageReceiver;
import com.jonahbauer.qed.networking.pages.QEDGalleryPages;
import com.jonahbauer.qed.util.Preferences;
import com.jonahbauer.qed.util.StatusWrapper;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ImageInfoViewModel extends AndroidViewModel implements QEDPageReceiver<Image> {
    private static final String LOG_TAG = ImageInfoViewModel.class.getName();

    private final AlbumDao mAlbumDao;
    private final MutableLiveData<StatusWrapper<Image>> mImage = new MutableLiveData<>();

    private final CompositeDisposable mDisposable = new CompositeDisposable();

    public ImageInfoViewModel(@NonNull Application application) {
        super(application);
        this.mAlbumDao = Database.getInstance(application).albumDao();
    }

    public void load(@NonNull Image image) {
        if (!image.isLoaded()) {
            this.mImage.setValue(StatusWrapper.wrap(image, STATUS_PRELOADED));
            mDisposable.add(
                    mAlbumDao.findImageById(image.getId())
                             .subscribeOn(Schedulers.io())
                             .observeOn(AndroidSchedulers.mainThread())
                             .subscribe(
                                     img -> {
                                         image.set(img);
                                         image.setDatabaseLoaded(true);

                                         if (image.isLoaded()) {
                                             onPageReceived(image);
                                         } else {
                                             loadFromInternet(image);
                                         }
                                     },
                                     err -> {
                                         Log.e(LOG_TAG, "Error reading image from database.", err);
                                         loadFromInternet(image);
                                     }
                             )
            );
        } else {
            onPageReceived(image);
        }
    }

    public LiveData<StatusWrapper<Image>> getImage() {
        return mImage;
    }

    private void loadFromInternet(Image image) {
        if (Preferences.gallery().isOfflineMode()) {
            onError(image, Reason.USER, null);
        } else {
            mDisposable.add(
                    QEDGalleryPages.getImageInfo(image, this)
            );
        }
    }

    @Override
    public void onPageReceived(@NonNull Image out) {
        if (!out.isLoaded()) {
            //noinspection ResultOfMethodCallIgnored
            this.mAlbumDao.insertOrUpdateImage(out)
                          .subscribeOn(Schedulers.io())
                          .observeOn(AndroidSchedulers.mainThread())
                          .subscribe(
                                  () -> {},
                                  err -> Log.e(LOG_TAG, "Could not save image info to database.", err)
                          );
        }

        out.setLoaded(true);
        this.mImage.setValue(StatusWrapper.wrap(out, STATUS_LOADED));
    }

    @Override
    public void onError(Image out, @NonNull Reason reason, @Nullable Throwable cause) {
        QEDPageReceiver.super.onError(out, reason, cause);
        this.mImage.setValue(StatusWrapper.wrap(out, STATUS_ERROR, reason));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mDisposable.clear();
    }
}