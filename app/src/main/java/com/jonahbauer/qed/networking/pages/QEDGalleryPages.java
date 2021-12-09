package com.jonahbauer.qed.networking.pages;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jonahbauer.qed.model.Album;
import com.jonahbauer.qed.model.Image;
import com.jonahbauer.qed.model.parser.AlbumListParser;
import com.jonahbauer.qed.model.parser.AlbumParser;
import com.jonahbauer.qed.model.parser.ImageParser;
import com.jonahbauer.qed.networking.Feature;
import com.jonahbauer.qed.networking.NetworkConstants;
import com.jonahbauer.qed.networking.async.AsyncLoadQEDPage;
import com.jonahbauer.qed.networking.async.AsyncLoadQEDPageToImage;
import com.jonahbauer.qed.networking.async.AsyncLoadQEDPageToStream;
import com.jonahbauer.qed.networking.async.QEDPageReceiver;
import com.jonahbauer.qed.networking.async.QEDPageStreamReceiver;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.CheckReturnValue;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.experimental.UtilityClass;

@UtilityClass
public class QEDGalleryPages extends QEDPages {

    @NonNull
    @CheckReturnValue
    public static Disposable getAlbumList(QEDPageReceiver<List<Album>> albumListReceiver) {
        AsyncLoadQEDPage network = new AsyncLoadQEDPage(
                Feature.GALLERY,
                NetworkConstants.GALLERY_SERVER_LIST
        );

        return run(
                network,
                AlbumListParser.INSTANCE,
                albumListReceiver,
                new ArrayList<>()
        );
    }

    @NonNull
    @CheckReturnValue
    public static Disposable getAlbum(@NonNull Album album, @Nullable Album.Filter filter, QEDPageReceiver<Album> albumReceiver) {
        String filterString = "";
        if (filter != null && !filter.isEmpty()) {
            album = new Album(album.getId());
            filterString = filter.toString();
        }

        AsyncLoadQEDPage network = new AsyncLoadQEDPage(
                Feature.GALLERY,
                NetworkConstants.GALLERY_SERVER_ALBUM + album.getId() + filterString
        );

        return run(
                network,
                AlbumParser.INSTANCE,
                albumReceiver,
                album
        );
    }

    @NonNull
    @CheckReturnValue
    public static Disposable getImageInfo(@NonNull Image image, QEDPageReceiver<Image> imageInfoReceiver) {
        AsyncLoadQEDPage network = new AsyncLoadQEDPage(
                Feature.GALLERY,
                NetworkConstants.GALLERY_SERVER_IMAGE_INFO + image.getId()
        );

        return run(
                network,
                ImageParser.INSTANCE,
                imageInfoReceiver,
                image
        );
    }

    @NonNull
    @CheckReturnValue
    public static <T> Disposable getImage(@NonNull T tag, @NonNull Image image, @NonNull Mode mode, @NonNull OutputStream outputStream, QEDPageStreamReceiver<T> imageReceiver) {
        AsyncLoadQEDPageToStream network = new AsyncLoadQEDPageToStream(
                Feature.GALLERY,
                String.format(NetworkConstants.GALLERY_SERVER_IMAGE, mode.mQuery, image.getId()),
                outputStream
        );

        return run(
                network,
                imageReceiver,
                tag
        );
    }

    @NonNull
    public static Single<Optional<Bitmap>> getThumbnail(@NonNull Image image) {
        AsyncLoadQEDPageToImage network = new AsyncLoadQEDPageToImage(
                Feature.GALLERY,
                String.format(NetworkConstants.GALLERY_SERVER_IMAGE, Mode.THUMBNAIL.mQuery, image.getId())
        );

        return Single.fromCallable(network)
                     .subscribeOn(Schedulers.io())
                     .observeOn(AndroidSchedulers.mainThread());
    }

    public enum Mode {
        THUMBNAIL("thumbnail"), NORMAL("normal"), ORIGINAL("original");

        private final String mQuery;

        Mode(String query) {
            this.mQuery = query;
        }
    }
}
