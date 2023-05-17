package eu.jonahbauer.qed.model.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import eu.jonahbauer.qed.model.Image;
import eu.jonahbauer.qed.model.room.AlbumDao;
import eu.jonahbauer.qed.model.room.Database;
import eu.jonahbauer.qed.networking.Reason;
import eu.jonahbauer.qed.networking.async.QEDPageReceiver;
import eu.jonahbauer.qed.networking.pages.QEDGalleryPages;
import eu.jonahbauer.qed.util.Preferences;
import eu.jonahbauer.qed.util.StatusWrapper;

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
            this.mImage.setValue(StatusWrapper.preloaded(image));
            mDisposable.add(
                    mAlbumDao.findImageById(image.getId())
                             .subscribeOn(Schedulers.io())
                             .observeOn(AndroidSchedulers.mainThread())
                             .subscribe(
                                     img -> {
                                         image.set(img);
                                         image.setDatabaseLoaded(true);

                                         if (image.isLoaded()) {
                                             onResult(image);
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
            onResult(image);
        }
    }

    public LiveData<StatusWrapper<Image>> getImage() {
        return mImage;
    }

    private void loadFromInternet(Image image) {
        if (Preferences.getGallery().isOfflineMode()) {
            onError(image, Reason.USER, null);
        } else {
            mDisposable.add(
                    QEDGalleryPages.getImageInfo(image, this)
            );
        }
    }

    @Override
    public void onResult(@NonNull Image out) {
        if (!out.isLoaded()) {
            out.setLoaded(true);

            //noinspection ResultOfMethodCallIgnored
            this.mAlbumDao.insertOrUpdateImage(out)
                          .subscribeOn(Schedulers.io())
                          .observeOn(AndroidSchedulers.mainThread())
                          .subscribe(
                                  () -> {},
                                  err -> Log.e(LOG_TAG, "Could not save image info to database.", err)
                          );
        }

        this.mImage.setValue(StatusWrapper.loaded(out));
    }

    @Override
    public void onError(Image out, @NonNull Reason reason, @Nullable Throwable cause) {
        QEDPageReceiver.super.onError(out, reason, cause);
        this.mImage.setValue(StatusWrapper.error(out, reason));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mDisposable.clear();
    }
}