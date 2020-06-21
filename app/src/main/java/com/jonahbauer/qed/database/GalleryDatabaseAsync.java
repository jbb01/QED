package com.jonahbauer.qed.database;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jonahbauer.qed.qedgallery.album.Album;
import com.jonahbauer.qed.qedgallery.image.Image;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class GalleryDatabaseAsync extends AsyncTask<Void, Integer, Boolean> {
    private final Mode mMode;
    private final GalleryDatabaseHelper mDatabaseHelper;

    private List<?> mValues;
    private boolean mInsertOrUpdate;
    private GalleryDatabaseReceiver mReceiver;

    public static GalleryDatabaseAsync insertImages(GalleryDatabaseHelper databaseHelper, List<Image> images, boolean insertOrUpdate, @Nullable GalleryDatabaseReceiver receiver) {
        GalleryDatabaseAsync async = new GalleryDatabaseAsync(Mode.INSERT_ALL_IMAGES, databaseHelper);
        async.mValues = images;
        async.mInsertOrUpdate = insertOrUpdate;
        async.mReceiver = receiver;
        return async;
    }

    public static GalleryDatabaseAsync insertAlbums(GalleryDatabaseHelper databaseHelper, List<Album> albums, boolean insertOrUpdate, @Nullable GalleryDatabaseReceiver receiver) {
        GalleryDatabaseAsync async = new GalleryDatabaseAsync(Mode.INSERT_ALL_ALBUMS, databaseHelper);
        async.mValues = albums;
        async.mInsertOrUpdate = insertOrUpdate;
        async.mReceiver = receiver;
        return async;
    }

    private GalleryDatabaseAsync(Mode mode, GalleryDatabaseHelper databaseHelper) {
        this.mMode = mode;
        this.mDatabaseHelper = databaseHelper;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        switch (mMode) {
            case INSERT_ALL_IMAGES:
                // handles in factory methods
                //noinspection unchecked
                insertAllImages((List<Image>) mValues, mInsertOrUpdate);
                return true;
            case INSERT_ALL_ALBUMS:
                // handles in factory methods
                //noinspection unchecked
                insertAllAlbums((List<Album>) mValues, mInsertOrUpdate);
                return true;
        }
        return false;
    }

    private void insertAllImages(@NonNull List<Image> images, boolean insertOrUpdate) {
        try (SQLiteDatabase galleryWritable = mDatabaseHelper.getWritableDatabase()) {
            galleryWritable.beginTransaction();
            try {
                AtomicInteger i = new AtomicInteger();
                int j = images.size();

                for (Image image : images) {
                    ContentValues value = new ContentValues();
                    value.put(GalleryDatabaseContract.ImageEntry.COLUMN_NAME_ID, image.id);
                    if (image.name != null)
                        value.put(GalleryDatabaseContract.ImageEntry.COLUMN_NAME_NAME, image.name);
                    if (image.path != null)
                        value.put(GalleryDatabaseContract.ImageEntry.COLUMN_NAME_PATH, image.path);
                    if (image.album.id != 0)
                        value.put(GalleryDatabaseContract.ImageEntry.COLUMN_NAME_ALBUM_ID, image.album.id);
                    if (image.format != null)
                        value.put(GalleryDatabaseContract.ImageEntry.COLUMN_NAME_FORMAT, image.format);
                    if (image.owner != null)
                        value.put(GalleryDatabaseContract.ImageEntry.COLUMN_NAME_OWNER, image.owner);
                    if (image.albumName != null)
                        value.put(GalleryDatabaseContract.ImageEntry.COLUMN_NAME_ALBUM_NAME, image.albumName);

                    value.put(GalleryDatabaseContract.ImageEntry.COLUMN_NAME_ORDER, i.get());

                    long row = galleryWritable.insertWithOnConflict(GalleryDatabaseContract.ImageEntry.TABLE_NAME, null, value, SQLiteDatabase.CONFLICT_IGNORE);
                    if (insertOrUpdate && row == -1)
                        galleryWritable.update(GalleryDatabaseContract.ImageEntry.TABLE_NAME, value, GalleryDatabaseContract.ImageEntry.COLUMN_NAME_ID + "=" + image.id, null);

                    publishProgress(i.incrementAndGet(), j);
                }

                galleryWritable.setTransactionSuccessful();
            } finally {
                galleryWritable.endTransaction();
            }
        }
    }

    private void insertAllAlbums(@NonNull List<Album> albums, boolean insertOrUpdate) {
        try (SQLiteDatabase galleryWritable = mDatabaseHelper.getWritableDatabase()) {
            galleryWritable.beginTransaction();
            try {

                AtomicInteger i = new AtomicInteger();
                int j = albums.size();

                for (Album album : albums) {
                    ContentValues value = new ContentValues();
                    value.put(GalleryDatabaseContract.AlbumEntry.COLUMN_NAME_ID, album.id);
                    if (album.name != null)
                        value.put(GalleryDatabaseContract.AlbumEntry.COLUMN_NAME_NAME, album.name);
                    if (album.owner != null)
                        value.put(GalleryDatabaseContract.AlbumEntry.COLUMN_NAME_CREATOR_NAME, album.owner);
                    if (album.creationDate != null)
                        value.put(GalleryDatabaseContract.AlbumEntry.COLUMN_NAME_CREATION_DATE, album.creationDate);
                    if (album.dates != null)
                        value.put(GalleryDatabaseContract.AlbumEntry.COLUMN_NAME_DATES, album.dates.toString());
                    if (album.categories != null)
                        value.put(GalleryDatabaseContract.AlbumEntry.COLUMN_NAME_CATEGORIES, album.categories.toString());
                    if (album.persons != null)
                        value.put(GalleryDatabaseContract.AlbumEntry.COLUMN_NAME_PERSONS, album.persons.toString());

                    long row = galleryWritable.insertWithOnConflict(GalleryDatabaseContract.AlbumEntry.TABLE_NAME, null, value, SQLiteDatabase.CONFLICT_IGNORE);
                    if (insertOrUpdate && row == -1)
                        galleryWritable.update(GalleryDatabaseContract.AlbumEntry.TABLE_NAME, value, GalleryDatabaseContract.ImageEntry.COLUMN_NAME_ID + "=" + album.id, null);

                    publishProgress(i.incrementAndGet(), j);
                }

                galleryWritable.setTransactionSuccessful();
            } finally {
                galleryWritable.endTransaction();
            }
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {}

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (mMode == Mode.INSERT_ALL_IMAGES && (mReceiver != null)) mReceiver.onInsertAllUpdate(values[0], values[1]);
        else if (mMode == Mode.INSERT_ALL_ALBUMS && (mReceiver != null)) mReceiver.onInsertAllUpdate(values[0], values[1]);
    }

    enum Mode {
        INSERT_ALL_IMAGES, INSERT_ALL_ALBUMS
    }
}
