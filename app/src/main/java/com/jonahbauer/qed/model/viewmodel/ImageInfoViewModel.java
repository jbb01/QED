package com.jonahbauer.qed.model.viewmodel;

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
import com.jonahbauer.qed.networking.QEDGalleryPages;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.networking.async.QEDPageReceiver;
import com.jonahbauer.qed.util.Preferences;
import com.jonahbauer.qed.util.StatusWrapper;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static com.jonahbauer.qed.util.StatusWrapper.STATUS_ERROR;
import static com.jonahbauer.qed.util.StatusWrapper.STATUS_LOADED;
import static com.jonahbauer.qed.util.StatusWrapper.STATUS_PRELOADED;

public class ImageInfoViewModel extends AndroidViewModel implements QEDPageReceiver<Image> {
    private static final String LOG_TAG = ImageInfoViewModel.class.getName();

    private final AlbumDao mAlbumDao;
    private final MutableLiveData<StatusWrapper<Image>> mImage = new MutableLiveData<>();

    public ImageInfoViewModel(@NonNull Application application) {
        super(application);
        this.mAlbumDao = Database.getInstance(application).albumDao();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void load(@NonNull Image image) {
        if (!image.isLoaded()) {
            this.mImage.setValue(StatusWrapper.wrap(image, STATUS_PRELOADED));
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
            QEDGalleryPages.getImageInfo(image, this);
        }
    }

    @Override
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void onPageReceived(@NonNull Image out) {
        if (!out.isLoaded()) {
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
}