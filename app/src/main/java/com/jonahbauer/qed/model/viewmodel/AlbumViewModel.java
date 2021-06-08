package com.jonahbauer.qed.model.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.jonahbauer.qed.model.Album;
import com.jonahbauer.qed.networking.QEDGalleryPages;
import com.jonahbauer.qed.networking.async.QEDPageReceiver;
import com.jonahbauer.qed.util.StatusWrapper;

import java.util.Map;

import static com.jonahbauer.qed.util.StatusWrapper.STATUS_ERROR;
import static com.jonahbauer.qed.util.StatusWrapper.STATUS_LOADED;
import static com.jonahbauer.qed.util.StatusWrapper.STATUS_PRELOADED;

public class AlbumViewModel extends AndroidViewModel implements QEDPageReceiver<Album> {
    private final MutableLiveData<StatusWrapper<Album>> mAlbum = new MutableLiveData<>();

    public AlbumViewModel(@NonNull Application application) {
        super(application);
    }

    public void load(@NonNull Album album, @Nullable Map<QEDGalleryPages.Filter, String> filters) {
        if (!album.isLoaded()) {
            this.mAlbum.setValue(StatusWrapper.wrap(album, STATUS_PRELOADED));
            QEDGalleryPages.getAlbum(album, filters, this);
        } else {
            onPageReceived(album);
        }
    }

    public void load(@Nullable Map<QEDGalleryPages.Filter, String> filters) {
        if (mAlbum != null && mAlbum.getValue() != null && mAlbum.getValue().getValue() != null) {
            Album album = getAlbum().getValue().getValue();
            this.mAlbum.setValue(StatusWrapper.wrap(album, STATUS_PRELOADED));
            QEDGalleryPages.getAlbum(album, filters, this);
        }
    }

    public LiveData<StatusWrapper<Album>> getAlbum() {
        return mAlbum;
    }

    @Override
    public void onPageReceived(@NonNull Album out) {
        out.setImageListDownloaded(true);
        out.setLoaded(true);
        this.mAlbum.setValue(StatusWrapper.wrap(out, STATUS_LOADED));
    }

    @Override
    public void onError(Album out, String reason, @Nullable Throwable cause) {
        QEDPageReceiver.super.onError(out, reason, cause);
        this.mAlbum.setValue(StatusWrapper.wrap(out, STATUS_ERROR, reason));
    }
}