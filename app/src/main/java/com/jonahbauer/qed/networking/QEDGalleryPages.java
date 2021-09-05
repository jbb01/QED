package com.jonahbauer.qed.networking;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jonahbauer.qed.model.Album;
import com.jonahbauer.qed.model.Image;
import com.jonahbauer.qed.model.parser.AlbumListParser;
import com.jonahbauer.qed.model.parser.AlbumParser;
import com.jonahbauer.qed.model.parser.ImageParser;
import com.jonahbauer.qed.networking.async.AsyncLoadQEDPage;
import com.jonahbauer.qed.networking.async.AsyncLoadQEDPageToStream;
import com.jonahbauer.qed.networking.async.QEDPageReceiver;
import com.jonahbauer.qed.networking.async.QEDPageStreamReceiver;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.experimental.UtilityClass;

@UtilityClass
@SuppressWarnings("UnusedReturnValue")
public class QEDGalleryPages {
    public static AsyncTask<?,?,?> getAlbumList(QEDPageReceiver<List<Album>> albumListReceiver) {
        AsyncLoadQEDPage<List<Album>> async = new AsyncLoadQEDPage<>(
                Feature.GALLERY,
                NetworkConstants.GALLERY_SERVER_LIST,
                new ArrayList<>(),
                AlbumListParser.INSTANCE,
                albumListReceiver
        );

        async.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        return async;
    }

    public static AsyncTask<?,?,?> getAlbum(@NonNull Album album, @Nullable Map<Filter, String> filters, QEDPageReceiver<Album> albumReceiver) {
        StringBuilder filterStringBuilder = new StringBuilder();
        if (filters != null) {
            album = new Album(album.getId());
            for (Filter filter : filters.keySet()) {
                filterStringBuilder.append(filter.mQuery).append(filters.getOrDefault(filter,""));
            }
        }

        AsyncLoadQEDPage<Album> albumPage = new AsyncLoadQEDPage<>(
                Feature.GALLERY,
                NetworkConstants.GALLERY_SERVER_ALBUM + album.getId() + filterStringBuilder.toString(),
                album,
                AlbumParser.INSTANCE,
                albumReceiver
        );

        albumPage.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        return albumPage;
    }

    public static AsyncTask<?,?,?> getImageInfo(@NonNull Image image, QEDPageReceiver<Image> imageInfoReceiver) {
        AsyncLoadQEDPage<Image> info = new AsyncLoadQEDPage<>(
                Feature.GALLERY,
                NetworkConstants.GALLERY_SERVER_IMAGE_INFO + image.getId(),
                image,
                ImageParser.INSTANCE,
                imageInfoReceiver
        );

        info.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        return info;
    }

    public static <T> AsyncTask<?,?,?> getImage(@NonNull T tag, @NonNull Image image, @NonNull Mode mode, @NonNull OutputStream outputStream, QEDPageStreamReceiver<T> imageReceiver) {
        AsyncLoadQEDPageToStream<T> async = new AsyncLoadQEDPageToStream<>(
                Feature.GALLERY,
                String.format(NetworkConstants.GALLERY_SERVER_IMAGE, mode.mQuery, image.getId()),
                tag,
                imageReceiver,
                outputStream
        );

        async.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        return async;
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
