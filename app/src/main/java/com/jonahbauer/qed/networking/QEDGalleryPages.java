package com.jonahbauer.qed.networking;

import android.net.Uri;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.qeddb.person.Person;
import com.jonahbauer.qed.qedgallery.album.Album;
import com.jonahbauer.qed.qedgallery.image.Image;

import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public abstract class QEDGalleryPages {
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.GERMANY);
    private static final ThreadPoolExecutor imageTpe = (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());


    public static AsyncTask getAlbumList(String tag, QEDPageReceiver<List<Album>> albumListReceiver) {
        Application application = Application.getContext();

        AsyncLoadQEDPage<List<Album>> list = new AsyncLoadQEDPage<>(
                AsyncLoadQEDPage.Feature.GALLERY,
                application.getString(R.string.gallery_server_list),
                albumListReceiver,
                tag,
                (String string) -> {
                    List<Album> albums = new ArrayList<>();
                    Matcher matcher = Pattern.compile("<li><a href='album_view\\.php\\?albumid=([\\d]*)&amp;page=1'>([^<]*)</a></li>").matcher(string);
                    while (matcher.find()) {
                        String id = matcher.group(1);
                        String name = matcher.group(2);
                        Album album = new Album();
                        album.id = Integer.valueOf(id);
                        album.name = name;
                        albums.add(album);
                    }
                    return albums;
                },
                new HashMap<>()
        );

        list.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);

        return list;
    }

    public static AsyncTask getAlbum(String tag, @NonNull Album album, @Nullable Map<Filter, String> filters, QEDPageReceiver<Album> albumReceiver) {
        Application application = Application.getContext();

        final boolean filtersUsed;

        StringBuilder filterStringBuilder = new StringBuilder();
        if (filters != null) {
            filtersUsed = !filters.keySet().isEmpty();
            for (Filter filter : filters.keySet()) {
                filterStringBuilder.append(filter.query).append(filters.getOrDefault(filter,""));
            }
        } else {
            filtersUsed = false;
        }

        AsyncLoadQEDPage<Album> albumPage = new AsyncLoadQEDPage<>(
                AsyncLoadQEDPage.Feature.GALLERY,
                application.getString(R.string.gallery_server_album) + album.id + filterStringBuilder.toString(),
                albumReceiver,
                tag,
                (String string) -> {
                    Album albumTemp;
                    if (filtersUsed) albumTemp = new Album();
                    else albumTemp = album;

                    albumTemp.images.clear();
                    Matcher matcher = Pattern.compile("image\\.php\\?imageid=(\\d*)&amp;type=thumbnail' alt='([^']*)'").matcher(string);
                    while (matcher.find()) {
                        String id = matcher.group(1);
                        Image image = new Image();
                        image.id = Integer.valueOf(id);
                        image.album = album;
                        image.name = matcher.group(2);
                        albumTemp.images.add(image);
                    }

                    albumTemp.persons.clear();
                    Matcher matcher1 = Pattern.compile("album_view\\.php\\?albumid=\\d*&amp;page=1&amp;byowner=(\\d*)'>Bilder von ([^<]*)").matcher(string);
                    while (matcher1.find()) {
                        Person person = new Person();
                        person.id = Integer.valueOf(matcher1.group(1));
                        person.firstName = matcher1.group(2);
                        albumTemp.persons.add(person);
                    }

                    albumTemp.dates.clear();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY);
                    Matcher matcher2 = Pattern.compile("album_view\\.php\\?albumid=\\d*&amp;page=1&amp;byday=([\\d\\-]*)").matcher(string);
                    while (matcher2.find()) {
                        try {
                            albumTemp.dates.add(sdf.parse(matcher2.group(1)));
                        } catch (ParseException ignored) {}
                    }

                    albumTemp.categories.clear();
                    Matcher matcher3 = Pattern.compile("album_view\\.php\\?albumid=\\d*&amp;page=1&amp;bycategory=([^']*)").matcher(string);
                    while (matcher3.find()) {
                        if (matcher3.group(1).equals(""))
                            albumTemp.categories.add("Sonstige");
                        else
                            albumTemp.categories.add(Uri.decode(matcher3.group(1)));
                    }

                    Matcher matcher4 = Pattern.compile("<th>Albumersteller:</th><td>([^<]*)</td>").matcher(string);
                    if (matcher4.find()) {
                        albumTemp.owner = matcher4.group(1);
                    }

                    Matcher matcher5 = Pattern.compile("<th>Erstellt am:</th><td>([^<]*)</td>").matcher(string);
                    if (matcher5.find()) {
                        albumTemp.creationDate = matcher5.group(1);
                    }

                    return albumTemp;
                },
                new HashMap<>()
        );

        albumPage.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
        return albumPage;
    }

    public static AsyncTask getImageInfo(String tag, int imageId, QEDPageReceiver<Image> imageQEDPageReceiver) {
        Image image = new Image();
        image.id = imageId;
        return getImageInfo(tag, image, imageQEDPageReceiver);
    }

    public static AsyncTask getImageInfo(String tag, @NonNull Image image, QEDPageReceiver<Image> imageInfoReceiver) {
        Application application = Application.getContext();

        AsyncLoadQEDPage<Image> info = new AsyncLoadQEDPage<>(
                AsyncLoadQEDPage.Feature.GALLERY,
                application.getString(R.string.gallery_server_image_info) + image.id,
                imageInfoReceiver,
                tag,
                (String string) -> {
                    Matcher matcher = Pattern.compile("<th>([^<]*):</th><td>([^<]*)</td>").matcher(string);
                    while (matcher.find()) {
                        switch (matcher.group(1)) {
                            case "Besitzer":
                                image.owner = matcher.group(2);
                                break;
                            case "Dateiformat":
                                image.format = matcher.group(2);
                                break;
                            case "Hochgeladen am":
                                try {
                                    image.uploadDate = simpleDateFormat.parse(matcher.group(2));
                                } catch (ParseException ignored) {}
                                break;
                            case "Aufgenommen am":
                                try {
                                    image.creationDate = simpleDateFormat.parse(matcher.group(2));
                                } catch (ParseException ignored) {}
                                break;
                        }
                    }

                    Matcher matcher2 = Pattern.compile("<div><b>([^<]*)</b></div>").matcher(string);
                    if (matcher2.find()) {
                        image.name = matcher2.group(1);
                    }
                    return image;
                },
                new HashMap<>()
        );

        info.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
        return info;
    }

    public static AsyncTask getImage(String tag, @NonNull Image image, @NonNull Mode mode, @NonNull OutputStream outputStream, QEDPageStreamReceiver imageReceiver) {
        AsyncLoadQEDPageToStream async = new AsyncLoadQEDPageToStream(
                AsyncLoadQEDPageToStream.Feature.GALLERY,
                String.format(Application.getContext().getString(R.string.gallery_server_image), mode.query, String.valueOf(image.id)),
                imageReceiver,
                tag,
                outputStream
        );

        try {
            async.executeOnExecutor(imageTpe, (Void) null);
        } catch (RejectedExecutionException e) {
            imageReceiver.onStreamError(tag);
            return null;
        }
        return async;
    }

    public enum Filter {
        BY_DATE("&byday="), BY_PERSON("&byowner="), BY_CATEGORY("&bycategory=");

        private String query;

        Filter(String query) {
            this.query = query;
        }
    }

    public enum Mode {
        THUMBNAIL("thumbnail"), NORMAL("normal"), ORIGINAL("original");

        private String query;

        Mode(String query) {
            this.query = query;
        }
    }
}
