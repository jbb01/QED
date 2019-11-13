package com.jonahbauer.qed.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import com.jonahbauer.qed.qedgallery.album.Album;
import com.jonahbauer.qed.qedgallery.image.Image;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.jonahbauer.qed.database.GalleryDatabaseContract.AlbumEntry;
import static com.jonahbauer.qed.database.GalleryDatabaseContract.ImageEntry;

public class GalleryDatabase {
    private GalleryDatabaseHelper galleryDatabaseHelper;
    private int insertsRunning = 0;
    private List<GalleryDatabaseAsync> asyncTasks;
    private GalleryDatabaseReceiver receiver;

    public void init(Context context, GalleryDatabaseReceiver receiver) {
        galleryDatabaseHelper = new GalleryDatabaseHelper(context);
        asyncTasks = new ArrayList<>();
        this.receiver = receiver;
    }

    public void close() {
        if (galleryDatabaseHelper != null) galleryDatabaseHelper.close();
        asyncTasks.forEach(async -> {
            if (!async.isCancelled()) async.cancel(true);
        });
    }

    @SuppressWarnings("UnusedReturnValue")
    public long insert(@NonNull Album album, boolean insertOrUpdate) {
        insertsRunning ++;
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

        SQLiteDatabase galleryWritable = galleryDatabaseHelper.getWritableDatabase();

        row = galleryWritable.insertWithOnConflict(AlbumEntry.TABLE_NAME, null, value, SQLiteDatabase.CONFLICT_IGNORE);
        if (insertOrUpdate || row == -1) galleryWritable.update(AlbumEntry.TABLE_NAME, value, AlbumEntry.COLUMN_NAME_ID + "=" + album.id, null);

        insertsRunning --;
        if (insertsRunning == 0) galleryWritable.close();
        return row;
    }

    @SuppressWarnings("UnusedReturnValue")
    public long insert(@NonNull Image image, boolean insertOrUpdate) {
        insertsRunning ++;
        ContentValues value = new ContentValues();
        value.put(ImageEntry.COLUMN_NAME_ID, image.id);
        if (image.original) value.put(ImageEntry.COLUMN_NAME_IS_ORIGINAL, 1);
        if (image.name != null) value.put(ImageEntry.COLUMN_NAME_NAME, image.name);
        if (image.path != null) value.put(ImageEntry.COLUMN_NAME_PATH, image.path);
        if (image.thumbnailPath != null) value.put(ImageEntry.COLUMN_NAME_THUMBNAIL_PATH, image.thumbnailPath);
        if (image.album != null && image.album.id != 0) value.put(ImageEntry.COLUMN_NAME_ALBUM_ID, image.album.id);
        if (image.format != null) value.put(ImageEntry.COLUMN_NAME_FORMAT, image.format);
        if (image.owner != null) value.put(ImageEntry.COLUMN_NAME_OWNER, image.owner);
        if (image.uploadDate != null) value.put(ImageEntry.COLUMN_NAME_UPLOAD_DATE, image.uploadDate.getTime());
        if (image.creationDate != null) value.put(ImageEntry.COLUMN_NAME_CREATION_DATE, image.creationDate.getTime());
        if (image.albumName != null) value.put(ImageEntry.COLUMN_NAME_ALBUM_NAME, image.albumName);

        long row;

        SQLiteDatabase galleryWritable = galleryDatabaseHelper.getWritableDatabase();
        galleryWritable.beginTransaction();

        row = galleryWritable.insertWithOnConflict(ImageEntry.TABLE_NAME, null, value, SQLiteDatabase.CONFLICT_IGNORE);
        if (insertOrUpdate && row == -1) galleryWritable.update(ImageEntry.TABLE_NAME, value, ImageEntry.COLUMN_NAME_ID + "=" + image.id, null);

        galleryWritable.setTransactionSuccessful();
        galleryWritable.endTransaction();

        insertsRunning --;
        if (insertsRunning == 0) galleryWritable.close();
        return row;
    }

    public void insertAllImages(List<Image> list, boolean insertOrUpdate) {
        GalleryDatabaseAsync async = new GalleryDatabaseAsync();
        asyncTasks.add(async);
        async.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, GalleryDatabaseAsync.Mode.INSERT_ALL_IMAGES, galleryDatabaseHelper, list, insertOrUpdate, receiver);
    }

    public void insertAllAlbums(List<Album> list, boolean insertOrUpdate) {
        GalleryDatabaseAsync async = new GalleryDatabaseAsync();
        asyncTasks.add(async);
        async.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, GalleryDatabaseAsync.Mode.INSERT_ALL_ALBUMS, galleryDatabaseHelper, list, insertOrUpdate, receiver);
    }

    public String getImageThumbnailPath(@NonNull Image image) {
        SQLiteDatabase galleryReadable = galleryDatabaseHelper.getReadableDatabase();
        galleryReadable.beginTransaction();
        Cursor cursor = galleryReadable.query(ImageEntry.TABLE_NAME, new String[] {ImageEntry.COLUMN_NAME_THUMBNAIL_PATH}, ImageEntry.COLUMN_NAME_ID + "=?", new String[] {String.valueOf(image.id)}, null, null, null);

        String imageThumbnailPath = null;
        if (cursor.moveToFirst()) {
            imageThumbnailPath = cursor.getString(0);
        }

        cursor.close();
        galleryReadable.setTransactionSuccessful();
        galleryReadable.endTransaction();
        galleryReadable.close();

        return imageThumbnailPath;
    }

    public String getImagePath(@NonNull Image image) {
        SQLiteDatabase galleryReadable = galleryDatabaseHelper.getReadableDatabase();
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
        SQLiteDatabase galleryReadable = galleryDatabaseHelper.getReadableDatabase();
        galleryReadable.beginTransaction();
        Cursor cursor = galleryReadable.query(ImageEntry.TABLE_NAME, new String[] {ImageEntry.COLUMN_NAME_PATH, ImageEntry.COLUMN_NAME_UPLOAD_DATE, ImageEntry.COLUMN_NAME_FORMAT, ImageEntry.COLUMN_NAME_NAME, ImageEntry.COLUMN_NAME_OWNER, ImageEntry.COLUMN_NAME_THUMBNAIL_PATH, ImageEntry.COLUMN_NAME_IS_ORIGINAL, ImageEntry.COLUMN_NAME_CREATION_DATE, ImageEntry.COLUMN_NAME_ALBUM_NAME}, ImageEntry.COLUMN_NAME_ID + "=?", new String[] {String.valueOf(image.id)}, null, null, null);

        if (cursor.moveToFirst()) {
            image.path = cursor.getString(0);
            image.uploadDate = new Date(cursor.getLong(1));
            image.format = cursor.getString(2);
            image.name = cursor.getString(3);
            image.owner = cursor.getString(4);
            image.thumbnailPath = cursor.getString(5);
            image.albumName = cursor.getString(8);
            image.creationDate = new Date(cursor.getLong(7));
            boolean original = cursor.getInt(6) == 1;
            if (original) image.original = true;
        }

        cursor.close();
        galleryReadable.setTransactionSuccessful();
        galleryReadable.endTransaction();
        galleryReadable.close();

        return image;
    }

    @SuppressWarnings("unused")
    public void clearColumn(String table, String columnName) {
        SQLiteDatabase galleryWritable = galleryDatabaseHelper.getWritableDatabase();
        galleryWritable.beginTransaction();
        galleryWritable.execSQL("UPDATE TABLE " + table + " SET " + columnName + " = NULL");
        galleryWritable.setTransactionSuccessful();
        galleryWritable.endTransaction();
        galleryWritable.close();
    }

    public List<Album> getAlbums() {
        SQLiteDatabase galleryReadable = galleryDatabaseHelper.getReadableDatabase();
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
        SQLiteDatabase galleryReadable = galleryDatabaseHelper.getReadableDatabase();
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
        SQLiteDatabase galleryReadable = galleryDatabaseHelper.getReadableDatabase();

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
        SQLiteDatabase writableDatabase = galleryDatabaseHelper.getWritableDatabase();
        writableDatabase.beginTransaction();
        galleryDatabaseHelper.clear(writableDatabase);
        writableDatabase.setTransactionSuccessful();
        writableDatabase.endTransaction();
    }
}

