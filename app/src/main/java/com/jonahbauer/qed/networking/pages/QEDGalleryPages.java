package com.jonahbauer.qed.networking.pages;

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
import com.jonahbauer.qed.networking.async.AsyncLoadQEDPageToStream;
import com.jonahbauer.qed.networking.async.QEDPageReceiver;
import com.jonahbauer.qed.networking.async.QEDPageStreamReceiver;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.annotations.CheckReturnValue;
import io.reactivex.rxjava3.disposables.Disposable;
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
    public static Disposable getAlbum(@NonNull Album album, @Nullable Map<Filter, String> filters, QEDPageReceiver<Album> albumReceiver) {
        StringBuilder filterStringBuilder = new StringBuilder();
        if (filters != null) {
            album = new Album(album.getId());
            for (Filter filter : filters.keySet()) {
                filterStringBuilder.append(filter.mQuery).append(filters.getOrDefault(filter,""));
            }
        }

        AsyncLoadQEDPage network = new AsyncLoadQEDPage(
                Feature.GALLERY,
                NetworkConstants.GALLERY_SERVER_ALBUM + album.getId() + filterStringBuilder.toString()
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

    public enum Filter {
        BY_DATE("&byday="), BY_PERSON("&byowner="), BY_CATEGORY("&bycategory=");

        private final String mQuery;

        Filter(String query) {
            this.mQuery = query;
        }
    }

    public enum Mode {
        THUMBNAIL("thumbnail"), NORMAL("normal"), ORIGINAL("original");

        private final String mQuery;

        Mode(String query) {
            this.mQuery = query;
        }
    }
}
