package com.jonahbauer.qed.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.qedgallery.album.Album;
import com.jonahbauer.qed.qedgallery.image.Image;
import com.x5.util.Base64;

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

public class GalleryDatabase {
    private GalleryDatabaseHelper mGalleryDatabaseHelper;
    private List<GalleryDatabaseAsync> mAsyncTasks;
    private GalleryDatabaseReceiver mReceiver;

    public void init(Context context, GalleryDatabaseReceiver receiver) {
        mGalleryDatabaseHelper = new GalleryDatabaseHelper(context);
        mAsyncTasks = new ArrayList<>();
        this.mReceiver = receiver;
    }

    public void close() {
        if (mGalleryDatabaseHelper != null) mGalleryDatabaseHelper.close();
        mAsyncTasks.forEach(async -> {
            if (!async.isCancelled()) async.cancel(true);
        });
    }

    @SuppressWarnings({"UnusedReturnValue", "ConstantConditions"})
    public long insert(@NonNull Album album, boolean insertOrUpdate) {
        ContentValues value = new ContentValues();
        value.put(AlbumEntry.COLUMN_NAME_ID, album.id);
        if (album.imageListDownloaded) value.put(AlbumEntry.COLUMN_NAME_IMAGE_LIST_DOWNLOADED, true);
        if (album.name != null) value.put(AlbumEntry.COLUMN_NAME_NAME, album.name);
        if (album.owner != null) value.put(AlbumEntry.COLUMN_NAME_CREATOR_NAME, album.owner);
        if (album.creationDate != null) value.put(AlbumEntry.COLUMN_NAME_CREATION_DATE, album.creationDate);
        if (album.dates != null) value.put(AlbumEntry.COLUMN_NAME_DATES, album.dates.toString());
        if (album.categories != null) value.put(AlbumEntry.COLUMN_NAME_CATEGORIES, album.categories.toString());
        if (album.persons != null) value.put(AlbumEntry.COLUMN_NAME_PERSONS, album.persons.toString());


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
        value.put(ImageEntry.COLUMN_NAME_ID, image.id);
        if (image.original) value.put(ImageEntry.COLUMN_NAME_IS_ORIGINAL, 1);
        if (image.name != null) value.put(ImageEntry.COLUMN_NAME_NAME, image.name);
        if (image.path != null) value.put(ImageEntry.COLUMN_NAME_PATH, image.path);
        if (image.album != null && image.album.id != 0) value.put(ImageEntry.COLUMN_NAME_ALBUM_ID, image.album.id);
        if (image.format != null) value.put(ImageEntry.COLUMN_NAME_FORMAT, image.format);
        if (image.owner != null) value.put(ImageEntry.COLUMN_NAME_OWNER, image.owner);
        if (image.uploadDate != null) value.put(ImageEntry.COLUMN_NAME_UPLOAD_DATE, image.uploadDate.getTime());
        if (image.creationDate != null) value.put(ImageEntry.COLUMN_NAME_CREATION_DATE, image.creationDate.getTime());
        if (image.albumName != null) value.put(ImageEntry.COLUMN_NAME_ALBUM_NAME, image.albumName);
        if (image.data != null) value.put(ImageEntry.COLUMN_NAME_DATA, mapToString(image.data));

        long row;

        try (SQLiteDatabase galleryWritable = mGalleryDatabaseHelper.getWritableDatabase()) {
            row = galleryWritable.insertWithOnConflict(ImageEntry.TABLE_NAME, null, value, insertOrUpdate ? SQLiteDatabase.CONFLICT_REPLACE : SQLiteDatabase.CONFLICT_IGNORE);
//            if (insertOrUpdate && row == -1)
//                galleryWritable.update(ImageEntry.TABLE_NAME, value, ImageEntry.COLUMN_NAME_ID + "=" + image.id, null);
        }

        return row;
    }

    public void insertAllImages(List<Image> list, boolean insertOrUpdate) {
        GalleryDatabaseAsync async = GalleryDatabaseAsync.insertImages(mGalleryDatabaseHelper, list, insertOrUpdate, mReceiver);
        mAsyncTasks.add(async);
        async.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void insertAllAlbums(List<Album> list, boolean insertOrUpdate) {
        GalleryDatabaseAsync async = GalleryDatabaseAsync.insertAlbums(mGalleryDatabaseHelper, list, insertOrUpdate, mReceiver);
        mAsyncTasks.add(async);
        async.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public Bitmap getThumbnail(@NonNull Image image) {
        try (SQLiteDatabase galleryReadable = mGalleryDatabaseHelper.getReadableDatabase()) {
            try (Cursor cursor = galleryReadable.query(
                    ThumbEntry.TABLE_NAME,
                    new String[]{ThumbEntry.COLUMN_NAME_THUMBNAIL},
                    ThumbEntry.COLUMN_NAME_ID + "=?",
                    new String[]{String.valueOf(image.id)},
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
            values.put(ThumbEntry.COLUMN_NAME_ID, image.id);
            values.put(ThumbEntry.COLUMN_NAME_THUMBNAIL, storeImage(bitmap));

            galleryWritable.insert(ThumbEntry.TABLE_NAME, null, values);
        }
    }

    public String getImagePath(@NonNull Image image) {
        SQLiteDatabase galleryReadable = mGalleryDatabaseHelper.getReadableDatabase();
        galleryReadable.beginTransaction();
        Cursor cursor = galleryReadable.query(ImageEntry.TABLE_NAME, new String[] {ImageEntry.COLUMN_NAME_PATH}, ImageEntry.COLUMN_NAME_ID + "=?", new String[] {String.valueOf(image.id)}, null, null, null);

        String imagePath = null;
        if (cursor.moveToFirst()) {
            imagePath = cursor.getString(0);
        }

        cursor.close();
        galleryReadable.setTransactionSuccessful();
        galleryReadable.endTransaction();
        galleryReadable.close();

        return imagePath;
    }

    public Image getImageData(@NonNull Image image) {
        SQLiteDatabase galleryReadable = mGalleryDatabaseHelper.getReadableDatabase();
        galleryReadable.beginTransaction();
        Cursor cursor = galleryReadable.query(ImageEntry.TABLE_NAME, new String[] {ImageEntry.COLUMN_NAME_PATH, ImageEntry.COLUMN_NAME_UPLOAD_DATE, ImageEntry.COLUMN_NAME_FORMAT, ImageEntry.COLUMN_NAME_NAME, ImageEntry.COLUMN_NAME_OWNER, ImageEntry.COLUMN_NAME_IS_ORIGINAL, ImageEntry.COLUMN_NAME_CREATION_DATE, ImageEntry.COLUMN_NAME_ALBUM_NAME, ImageEntry.COLUMN_NAME_DATA}, ImageEntry.COLUMN_NAME_ID + "=?", new String[] {String.valueOf(image.id)}, null, null, null);

        if (cursor.moveToFirst()) {
            image.path = cursor.getString(0);
            image.uploadDate = new Date(cursor.getLong(1));
            image.format = cursor.getString(2);
            image.name = cursor.getString(3);
            image.owner = cursor.getString(4);
            image.albumName = cursor.getString(7);
            image.creationDate = new Date(cursor.getLong(6));
            image.data = stringToMap(cursor.getString(8));
            boolean original = cursor.getInt(5) == 1;
            if (original) image.original = true;
        }

        cursor.close();
        galleryReadable.setTransactionSuccessful();
        galleryReadable.endTransaction();
        galleryReadable.close();

        return image;
    }

    public List<Album> getAlbums() {
        SQLiteDatabase galleryReadable = mGalleryDatabaseHelper.getReadableDatabase();
        galleryReadable.beginTransaction();

        List<Album> out = new ArrayList<>(30);

        Cursor cursor = galleryReadable.query(AlbumEntry.TABLE_NAME, new String[] {AlbumEntry.COLUMN_NAME_ID, AlbumEntry.COLUMN_NAME_NAME, AlbumEntry.COLUMN_NAME_IMAGE_LIST_DOWNLOADED}, null, null, null, null, AlbumEntry.COLUMN_NAME_ID + " DESC");

        while (cursor.moveToNext()) {
            int id = cursor.getInt(0);
            String name = cursor.getString(1);
            boolean downloaded = cursor.getInt(2) == 1;

            Album album = new Album();
            album.id = id;
            album.name = name;
            album.imageListDownloaded = downloaded;

            out.add(album);
        }

        galleryReadable.setTransactionSuccessful();
        galleryReadable.endTransaction();
        cursor.close();

        return out;
    }

    public List<Image> getImageList(@NonNull Album album) {
        SQLiteDatabase galleryReadable = mGalleryDatabaseHelper.getReadableDatabase();
        galleryReadable.beginTransaction();

        List<Image> out = new ArrayList<>(50);

        Cursor cursor = galleryReadable.query(ImageEntry.TABLE_NAME, new String[] {ImageEntry.COLUMN_NAME_ID, ImageEntry.COLUMN_NAME_NAME}, ImageEntry.COLUMN_NAME_ALBUM_ID + "=" + album.id, null, null, null, ImageEntry.COLUMN_NAME_ORDER);

        while (cursor.moveToNext()) {
            int id = cursor.getInt(0);
            String name = cursor.getString(1);

            Image image = new Image();
            image.id = id;
            image.name = name;

            out.add(image);
        }

        galleryReadable.setTransactionSuccessful();
        galleryReadable.endTransaction();
        cursor.close();

        return out;
    }

    public void getAlbumData(@NonNull Album album) {
        SQLiteDatabase galleryReadable = mGalleryDatabaseHelper.getReadableDatabase();

        try (Cursor cursor = galleryReadable.query(AlbumEntry.TABLE_NAME, new String[] {AlbumEntry.COLUMN_NAME_NAME, AlbumEntry.COLUMN_NAME_CREATOR_NAME, AlbumEntry.COLUMN_NAME_CREATION_DATE}, AlbumEntry.COLUMN_NAME_ID + "=" + album.id, null, null, null, null, "1")) {
            if (cursor.moveToNext()) {
                String name = cursor.getString(0);
                String creatorName = cursor.getString(1);
                String creationDate = cursor.getString(2);

                if ((album.name == null || album.name.isEmpty()) && name != null) album.name = name;
                if ((album.owner == null || album.owner.isEmpty()) && creatorName != null) album.owner = creatorName;
                if ((album.creationDate == null || album.creationDate.isEmpty()) && creationDate != null) album.creationDate = creationDate;
            }
        }

        galleryReadable.close();
    }

    public void clear() {
        SQLiteDatabase writableDatabase = mGalleryDatabaseHelper.getWritableDatabase();
        writableDatabase.beginTransaction();
        mGalleryDatabaseHelper.clear(writableDatabase);
        writableDatabase.setTransactionSuccessful();
        writableDatabase.endTransaction();
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

