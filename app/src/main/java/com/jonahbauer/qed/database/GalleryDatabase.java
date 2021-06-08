package com.jonahbauer.qed.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.model.Album;
import com.jonahbauer.qed.model.Image;
import com.x5.util.Base64;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.jonahbauer.qed.database.GalleryDatabaseContract.AlbumEntry;
import static com.jonahbauer.qed.database.GalleryDatabaseContract.ImageEntry;
import static com.jonahbauer.qed.database.GalleryDatabaseContract.ThumbEntry;

public class GalleryDatabase implements AutoCloseable {
    private GalleryDatabaseHelper mGalleryDatabaseHelper;
    private List<GalleryDatabaseAsync> mAsyncTasks;

    public void init(Context context) {
        mGalleryDatabaseHelper = new GalleryDatabaseHelper(context);
        mAsyncTasks = new ArrayList<>();
    }

    public void close() {
        if (mGalleryDatabaseHelper != null) mGalleryDatabaseHelper.close();
        mAsyncTasks.forEach(async -> {
            if (!async.isCancelled()) async.cancel(true);
        });
    }

    @SuppressWarnings({"UnusedReturnValue"})
    public long insert(@NonNull Album album, boolean insertOrUpdate) {
        ContentValues value = new ContentValues();
        value.put(AlbumEntry.COLUMN_NAME_ID, album.getId());
        if (album.isImageListDownloaded()) value.put(AlbumEntry.COLUMN_NAME_IMAGE_LIST_DOWNLOADED, true);
        if (album.getName() != null) value.put(AlbumEntry.COLUMN_NAME_NAME, album.getName());
        if (album.getOwner() != null) value.put(AlbumEntry.COLUMN_NAME_CREATOR_NAME, album.getOwner());
        if (album.getCreationDate() != null) value.put(AlbumEntry.COLUMN_NAME_CREATION_DATE, album.getCreationDate());
        if (album.getDates() != null) value.put(AlbumEntry.COLUMN_NAME_DATES, album.getDates().toString());
        if (album.getCategories() != null) value.put(AlbumEntry.COLUMN_NAME_CATEGORIES, album.getCategories().toString());
        if (album.getPersons() != null) value.put(AlbumEntry.COLUMN_NAME_PERSONS, album.getPersons().toString());


        long row;

        try (SQLiteDatabase galleryWritable = mGalleryDatabaseHelper.getWritableDatabase()) {
            row = galleryWritable.insertWithOnConflict(AlbumEntry.TABLE_NAME, null, value, insertOrUpdate ? SQLiteDatabase.CONFLICT_REPLACE : SQLiteDatabase.CONFLICT_IGNORE);
//            if (insertOrUpdate || row == -1)
//                galleryWritable.update(AlbumEntry.TABLE_NAME, value, AlbumEntry.COLUMN_NAME_ID + "=" + album.id, null);
        }

        return row;
    }

    @SuppressWarnings("UnusedReturnValue")
    public long insert(@NonNull Image image, boolean insertOrUpdate) {
        ContentValues value = new ContentValues();
        value.put(ImageEntry.COLUMN_NAME_ID, image.getId());
        if (image.isOriginal()) value.put(ImageEntry.COLUMN_NAME_IS_ORIGINAL, 1);
        if (image.getName() != null) value.put(ImageEntry.COLUMN_NAME_NAME, image.getName());
        if (image.getPath() != null) value.put(ImageEntry.COLUMN_NAME_PATH, image.getPath());
        if (image.getAlbum() != null && image.getAlbum().getId() != 0) value.put(ImageEntry.COLUMN_NAME_ALBUM_ID, image.getAlbum().getId());
        if (image.getFormat() != null) value.put(ImageEntry.COLUMN_NAME_FORMAT, image.getFormat());
        if (image.getOwner() != null) value.put(ImageEntry.COLUMN_NAME_OWNER, image.getOwner());
        if (image.getUploadDate() != null) value.put(ImageEntry.COLUMN_NAME_UPLOAD_DATE, image.getUploadDate().getTime());
        if (image.getCreationDate() != null) value.put(ImageEntry.COLUMN_NAME_CREATION_DATE, image.getCreationDate().getTime());
        if (image.getAlbumName() != null) value.put(ImageEntry.COLUMN_NAME_ALBUM_NAME, image.getAlbumName());
        if (image.getData() != null) value.put(ImageEntry.COLUMN_NAME_DATA, mapToString(image.getData()));

        long row;

        try (SQLiteDatabase galleryWritable = mGalleryDatabaseHelper.getWritableDatabase()) {
            row = galleryWritable.insertWithOnConflict(ImageEntry.TABLE_NAME, null, value, insertOrUpdate ? SQLiteDatabase.CONFLICT_REPLACE : SQLiteDatabase.CONFLICT_IGNORE);
//            if (insertOrUpdate && row == -1)
//                galleryWritable.update(ImageEntry.TABLE_NAME, value, ImageEntry.COLUMN_NAME_ID + "=" + image.id, null);
        }

        return row;
    }

    public void insertAllImages(List<Image> list, boolean insertOrUpdate, GalleryDatabaseReceiver receiver) {
        GalleryDatabaseAsync async = GalleryDatabaseAsync.insertImages(mGalleryDatabaseHelper, list, insertOrUpdate, receiver);
        mAsyncTasks.add(async);
        async.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void insertAllAlbums(List<Album> list, boolean insertOrUpdate, GalleryDatabaseReceiver receiver) {
        GalleryDatabaseAsync async = GalleryDatabaseAsync.insertAlbums(mGalleryDatabaseHelper, list, insertOrUpdate, receiver);
        mAsyncTasks.add(async);
        async.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public Bitmap getThumbnail(@NonNull Image image) {
        try (SQLiteDatabase galleryReadable = mGalleryDatabaseHelper.getReadableDatabase()) {
            try (Cursor cursor = galleryReadable.query(
                    ThumbEntry.TABLE_NAME,
                    new String[]{ThumbEntry.COLUMN_NAME_THUMBNAIL},
                    ThumbEntry.COLUMN_NAME_ID + "=?",
                    new String[]{String.valueOf(image.getId())},
                    null, null, null)) {
                if (cursor.moveToFirst()) {
                    byte[] encodedThumbnail = cursor.getBlob(0);
                    return loadImage(encodedThumbnail);
                }
            }
        }

        return null;
    }

    public void insertThumbnail(@NonNull Image image, @NonNull Bitmap bitmap) {
        try (SQLiteDatabase galleryWritable = mGalleryDatabaseHelper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(ThumbEntry.COLUMN_NAME_ID, image.getId());
            values.put(ThumbEntry.COLUMN_NAME_THUMBNAIL, storeImage(bitmap));

            galleryWritable.insert(ThumbEntry.TABLE_NAME, null, values);
        }
    }

    public void clearThumbnails() {
        try (SQLiteDatabase galleryWritable = mGalleryDatabaseHelper.getWritableDatabase()) {
            galleryWritable.beginTransaction();
            try {
                mGalleryDatabaseHelper.clearThumbnails(galleryWritable);
                galleryWritable.setTransactionSuccessful();
            } finally {
                galleryWritable.endTransaction();
            }
        }
    }

    public String getImagePath(@NonNull Image image) {
        try (SQLiteDatabase galleryReadable = mGalleryDatabaseHelper.getReadableDatabase()) {
            try (Cursor cursor = galleryReadable.query(
                    ImageEntry.TABLE_NAME,
                    new String[]{ImageEntry.COLUMN_NAME_PATH}, ImageEntry.COLUMN_NAME_ID + "=?",
                    new String[]{String.valueOf(image.getId())},
                    null, null, null)) {

                if (cursor.moveToFirst()) {
                    return cursor.getString(0);
                }
            }
        }

        return null;
    }

    public void getImageData(@NonNull Image image) {
        try (SQLiteDatabase galleryReadable = mGalleryDatabaseHelper.getReadableDatabase()) {
            try (Cursor cursor = galleryReadable.query(ImageEntry.TABLE_NAME, new String[]{ImageEntry.COLUMN_NAME_PATH, ImageEntry.COLUMN_NAME_UPLOAD_DATE, ImageEntry.COLUMN_NAME_FORMAT, ImageEntry.COLUMN_NAME_NAME, ImageEntry.COLUMN_NAME_OWNER, ImageEntry.COLUMN_NAME_IS_ORIGINAL, ImageEntry.COLUMN_NAME_CREATION_DATE, ImageEntry.COLUMN_NAME_ALBUM_NAME, ImageEntry.COLUMN_NAME_DATA}, ImageEntry.COLUMN_NAME_ID + "=?", new String[]{String.valueOf(image.getId())}, null, null, null)) {
                if (cursor.moveToFirst()) {
                    image.setPath(cursor.getString(0));
                    image.setUploadDate(new Date(cursor.getLong(1)));
                    image.setFormat(cursor.getString(2));
                    image.setName(cursor.getString(3));
                    image.setOwner(cursor.getString(4));
                    image.setAlbumName(cursor.getString(7));
                    image.setCreationDate(new Date(cursor.getLong(6)));
                    image.getData().putAll(stringToMap(cursor.getString(8)));
                    boolean original = cursor.getInt(5) == 1;
                    if (original) image.setOriginal(true);
                }
            }
        }
    }

    public List<Album> getAlbums() {
        try (SQLiteDatabase galleryReadable = mGalleryDatabaseHelper.getReadableDatabase()) {
            List<Album> out = new ArrayList<>(30);

            try (Cursor cursor = galleryReadable.query(
                    AlbumEntry.TABLE_NAME,
                    new String[]{AlbumEntry.COLUMN_NAME_ID, AlbumEntry.COLUMN_NAME_NAME, AlbumEntry.COLUMN_NAME_IMAGE_LIST_DOWNLOADED},
                    null, null, null, null,
                    AlbumEntry.COLUMN_NAME_ID + " DESC")) {

                while (cursor.moveToNext()) {
                    int id = cursor.getInt(0);
                    String name = cursor.getString(1);
                    boolean downloaded = cursor.getInt(2) == 1;

                    Album album = new Album(id);
                    album.setName(name);
                    album.setImageListDownloaded(downloaded);

                    out.add(album);
                }
            }

            return out;
        }
    }

    public List<Image> getImageList(@NonNull Album album) {
        try (SQLiteDatabase galleryReadable = mGalleryDatabaseHelper.getReadableDatabase()) {
            List<Image> out = new ArrayList<>(50);

            try (Cursor cursor = galleryReadable.query(
                    ImageEntry.TABLE_NAME,
                    new String[]{ImageEntry.COLUMN_NAME_ID, ImageEntry.COLUMN_NAME_NAME},
                    ImageEntry.COLUMN_NAME_ALBUM_ID + "=" + album.getId(),
                    null, null, null,
                    ImageEntry.COLUMN_NAME_ORDER)) {

                while (cursor.moveToNext()) {
                    int id = cursor.getInt(0);
                    String name = cursor.getString(1);

                    Image image = new Image(id);
                    image.setName(name);
                    image.setAlbum(album);

                    out.add(image);
                }
            }

            return out;
        }
    }

    public boolean getAlbumData(@NonNull Album album) {
        try (SQLiteDatabase galleryReadable = mGalleryDatabaseHelper.getReadableDatabase()) {
            try (Cursor cursor = galleryReadable.query(AlbumEntry.TABLE_NAME, new String[]{AlbumEntry.COLUMN_NAME_NAME, AlbumEntry.COLUMN_NAME_CREATOR_NAME, AlbumEntry.COLUMN_NAME_CREATION_DATE, AlbumEntry.COLUMN_NAME_IMAGE_LIST_DOWNLOADED}, AlbumEntry.COLUMN_NAME_ID + "=" + album.getId(), null, null, null, null, "1")) {
                if (cursor.moveToNext()) {
                    String name = cursor.getString(0);
                    String creatorName = cursor.getString(1);
                    String creationDate = cursor.getString(2);

                    if (StringUtils.isEmpty(album.getName()) && name != null)
                        album.setName(name);
                    if (StringUtils.isEmpty(album.getOwner()) && creatorName != null)
                        album.setOwner(creatorName);
                    if (StringUtils.isEmpty(album.getCreationDate()) && creationDate != null)
                        album.setCreationDate(creationDate);

                    return !cursor.isNull(3) && cursor.getInt(3) != 0;
                } else {
                    return false;
                }
            }
        }
    }

    public void clear() {
        try (SQLiteDatabase writableDatabase = mGalleryDatabaseHelper.getWritableDatabase()) {
            writableDatabase.beginTransaction();
            try {
                mGalleryDatabaseHelper.clear(writableDatabase);
                writableDatabase.setTransactionSuccessful();
            } finally {
                writableDatabase.endTransaction();
            }
        }
    }

    private static String mapToString(@NotNull Map<String,String> map) {
        return map.keySet().stream()
                .map(key -> Base64.encodeString(key) + ":" + Base64.encodeString("" + map.get(key)))
                .collect(Collectors.joining(", ", "{", "}"));
    }

    private static Map<String,String> stringToMap(String string) {
        if (string == null || string.matches("(\\s|\\n|\\r)*")) return new HashMap<>();
        string = string.substring(1, string.length() - 1);
        if (string.matches("(\\s|\\n|\\r)*")) return new HashMap<>();

        return Arrays.stream(string.split(","))
                .map(entry -> entry.trim().split(":"))
                //.peek(entry -> Log.d(Application.LOG_TAG_DEBUG, "stringToMapEntry: " + Base64.decodeToString(entry[0]) + " = " + Base64.decodeToString(entry[1])))
                .collect(Collectors.toMap(entry -> Base64.decodeToString(entry[0].trim()), entry -> Base64.decodeToString(entry[1].trim())));
    }

    private static Bitmap loadImage(byte[] encoded) {
        return BitmapFactory.decodeByteArray(encoded, 0, encoded.length);
    }

    @NonNull
    private static byte[] storeImage(@NonNull Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream.toByteArray();
    }
}

