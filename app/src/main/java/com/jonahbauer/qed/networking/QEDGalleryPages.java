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
import java.lang.ref.SoftReference;
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

    @Nullable
    public static AsyncTask getAlbumList(String tag, QEDPageReceiver<List<Album>> albumListReceiver) {
        SoftReference<Application> applicationReference = Application.getApplicationReference();
        Application application = applicationReference.get();

        if (application == null) {
            albumListReceiver.onError(tag, "", new Exception("Application is null!"));
            return null;
        }

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
                        try {
                            album.id = id != null ? Integer.parseInt(id) : 0;
                        } catch (NumberFormatException ignored) {}
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

    @Nullable
    public static AsyncTask getAlbum(String tag, @NonNull Album album, @Nullable Map<Filter, String> filters, QEDPageReceiver<Album> albumReceiver) {
        SoftReference<Application> applicationReference = Application.getApplicationReference();
        Application application = applicationReference.get();

        if (application == null) {
            albumReceiver.onError(tag, "", new Exception("Application is null!"));
            return null;
        }

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

                    if (string.contains("AlbumNotFoundException")) return null;


                    Matcher matcher0 = Pattern.compile("<h2>([^<]*) - Alle Bilder</h2>").matcher(string);
                    if (matcher0.find())
                        albumTemp.name = matcher0.group(1);

                    albumTemp.images.clear();
                    Matcher matcher = Pattern.compile("image\\.php\\?imageid=(\\d*)&amp;type=thumbnail' alt='([^']*)'").matcher(string);
                    while (matcher.find()) {
                        String id = matcher.group(1);
                        Image image = new Image();
                        try {
                            image.id = id != null ? Integer.parseInt(id) : 0;
                        } catch (NumberFormatException ignored) {}
                        image.album = album;
                        image.albumName = album.name;
                        image.name = matcher.group(2);
                        albumTemp.images.add(image);
                    }

                    albumTemp.persons.clear();
                    Matcher matcher1 = Pattern.compile("album_view\\.php\\?albumid=\\d*&amp;page=1&amp;byowner=(\\d*)'>Bilder von ([^<]*)").matcher(string);
                    while (matcher1.find()) {
                        String group1 = matcher1.group(1);

                        Person person = new Person();
                        try {
                            person.id = group1 != null ? Integer.parseInt(group1) : 0;
                        } catch (NumberFormatException ignored) {}
                        person.firstName = matcher1.group(2);
                        albumTemp.persons.add(person);
                    }

                    albumTemp.dates.clear();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY);
                    Matcher matcher2 = Pattern.compile("album_view\\.php\\?albumid=\\d*&amp;page=1&amp;byday=([\\d\\-]*)").matcher(string);
                    while (matcher2.find()) {
                        try {
                            String group1 = matcher2.group(1);
                            albumTemp.dates.add(group1 != null ? sdf.parse(group1) : null);
                        } catch (ParseException ignored) {}
                    }

                    albumTemp.categories.clear();
                    Matcher matcher3 = Pattern.compile("album_view\\.php\\?albumid=\\d*&amp;page=1&amp;bycategory=([^']*)").matcher(string);
                    while (matcher3.find()) {
                        if ("".equals(matcher3.group(1)))
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

    @Nullable
    public static AsyncTask getImageInfo(String tag, @NonNull Image image, QEDPageReceiver<Image> imageInfoReceiver) {
        SoftReference<Application> applicationReference = Application.getApplicationReference();
        Application application = applicationReference.get();

        if (application == null) {
            imageInfoReceiver.onError(tag, "", new Exception("Application is null!"));
            return null;
        }

        AsyncLoadQEDPage<Image> info = new AsyncLoadQEDPage<>(
                AsyncLoadQEDPage.Feature.GALLERY,
                application.getString(R.string.gallery_server_image_info) + image.id,
                imageInfoReceiver,
                tag,
                (String string) -> {
                    Matcher matcher = Pattern.compile("<th>([^<]*):</th><td>([^<]*)</td>").matcher(string);
                    while (matcher.find()) {
                        String group1 = matcher.group(1);
                        String group2 = matcher.group(2);

                        if (group1 != null) switch (group1) {
                            case "Album":
                                image.albumName = group2;
                                break;
                            case "Besitzer":
                                image.owner = group2;
                                break;
                            case "Dateiformat":
                                image.format = group2;
                                break;
                            case "Hochgeladen am":
                                try {
                                    image.uploadDate = group2 != null ? simpleDateFormat.parse(group2) : null;
                                } catch (ParseException ignored) {}
                                break;
                            case "Aufgenommen am":
                                try {
                                    image.creationDate = group2 != null ? simpleDateFormat.parse(group2) : null;
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

    @Nullable
    public static AsyncTask getImage(String tag, @NonNull Image image, @NonNull Mode mode, @NonNull OutputStream outputStream, QEDPageStreamReceiver imageReceiver) {
        SoftReference<Application> applicationReference = Application.getApplicationReference();
        Application application = applicationReference.get();

        if (application == null) {
            imageReceiver.onError(tag, "", new Exception("Application is null!"));
            return null;
        }

        AsyncLoadQEDPageToStream async = new AsyncLoadQEDPageToStream(
                AsyncLoadQEDPageToStream.Feature.GALLERY,
                String.format(application.getString(R.string.gallery_server_image), mode.query, String.valueOf(image.id)),
                imageReceiver,
                tag,
                outputStream
        );

        try {
            async.executeOnExecutor(imageTpe, (Void) null);
        } catch (RejectedExecutionException e) {
            imageReceiver.onError(tag, null, e);
            return null;
        }
        return async;
    }

    public enum Filter {
        BY_DATE("&byday="), BY_PERSON("&byowner="), BY_CATEGORY("&bycategory=");

        private final String query;

        Filter(String query) {
            this.query = query;
        }
    }

    public enum Mode {
        THUMBNAIL("thumbnail"), NORMAL("normal"), ORIGINAL("original");

        private final String query;

        Mode(String query) {
            this.query = query;
        }
    }
}
