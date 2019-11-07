package com.jonahbauer.qed.database;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import com.jonahbauer.qed.qedgallery.album.Album;
import com.jonahbauer.qed.qedgallery.image.Image;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class GalleryDatabaseAsync extends AsyncTask<Object, Integer, Boolean> {
    private static int insertsRunning = 0;
    private GalleryDatabaseHelper databaseHelper;
    private Mode mode;
    private GalleryDatabaseReceiver receiver;

    @Override
    protected Boolean doInBackground(Object... objects) {
        if (objects.length < 2) return false;
        else {
            if (objects[0] instanceof Mode) mode = (Mode) objects[0];
            else return false;

            if (objects[1] instanceof GalleryDatabaseHelper) databaseHelper = (GalleryDatabaseHelper) objects[1];
            else return false;
        }

        switch (mode) {
//            case QUERY:
//                if (objects.length < 4) return false;
//                else {
//                    String query;
//                    String[] args;
//
//                    if (objects[2] instanceof GalleryDatabaseReceiver) receiver = (GalleryDatabaseReceiver) objects[2];
//                    else return false;
//
//                    if (objects[3] instanceof String) query = (String) objects[3];
//                    else return false;
//
//                    if (objects.length >= 5) {
//                        if (objects[4] != null) {
//                            if (objects[4] instanceof String[]) args = (String[]) objects[4];
//                            else args = null;
//                        } else args = null;
//                    } else args = null;
//
//                    query(receiver, query, args);
//                    return true;
//                }
            case INSERT_ALL_IMAGES:
                if (objects.length < 4) return false;
                else {
                    List images;
                    boolean insertOrUpdate = false;

                    if (objects[2] instanceof List) images = (List) objects[2];
                    else return false;

                    if (objects[3] instanceof Boolean) insertOrUpdate = (Boolean) objects[3];

                    if (objects.length >= 5) if (objects[4] instanceof GalleryDatabaseReceiver) receiver = (GalleryDatabaseReceiver) objects[4];


                    insertAllImages(images, insertOrUpdate);
                    return true;
                }
            case INSERT_ALL_ALBUMS:
                if (objects.length < 4) return false;
                else {
                    List albums;
                    boolean insertOrUpdate = false;

                    if (objects[2] instanceof List) albums = (List) objects[2];
                    else return false;

                    if (objects[3] instanceof Boolean) insertOrUpdate = (Boolean) objects[3];

                    if (objects.length >= 5) if (objects[4] instanceof GalleryDatabaseReceiver) receiver = (GalleryDatabaseReceiver) objects[4];


                    insertAllAlbums(albums, insertOrUpdate);
                    return true;
                }
        }
        return true;
    }

//    private void query(GalleryDatabaseReceiver receiver, String query, String[] args) {
//        SQLiteDatabase database = databaseHelper.getReadableDatabase();
//
//        Cursor cursor = database.rawQuery(query, args);
//
//        List<Image> images = new ArrayList<>();
//
//        while (cursor.moveToNext()) {
//            images.add(new Message(
//                    cursor.getString(cursor.getColumnIndex(COLUMN_NAME_NAME)),
//                    cursor.getString(cursor.getColumnIndex(COLUMN_NAME_MESSAGE)),
//                    cursor.getString(cursor.getColumnIndex(COLUMN_NAME_DATE)),
//                    cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_USERID)),
//                    cursor.getString(cursor.getColumnIndex(COLUMN_NAME_USERNAME)),
//                    cursor.getString(cursor.getColumnIndex(COLUMN_NAME_COLOR)),
//                    cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_ID)),
//                    cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_BOTTAG)),
//                    cursor.getString(cursor.getColumnIndex(COLUMN_NAME_CHANNEL))
//            ));
//        }
//
//        cursor.close();
//        database.close();
//
//        receiver.onReceiveResult(images);
//    }

    private void insertAllImages(List<Image> images, boolean insertOrUpdate) {
        insertsRunning ++;
        SQLiteDatabase galleryWritable = databaseHelper.getWritableDatabase();

        galleryWritable.beginTransaction();

        AtomicInteger i = new AtomicInteger();
        int j = images.size();

        for (Image image : images) {
            ContentValues value = new ContentValues();
            value.put(GalleryDatabaseContract.ImageEntry.COLUMN_NAME_ID, image.id);
            if (image.name != null) value.put(GalleryDatabaseContract.ImageEntry.COLUMN_NAME_NAME, image.name);
            if (image.path != null) value.put(GalleryDatabaseContract.ImageEntry.COLUMN_NAME_PATH, image.path);
            if (image.thumbnailPath != null) value.put(GalleryDatabaseContract.ImageEntry.COLUMN_NAME_THUMBNAIL_PATH, image.thumbnailPath);
            if (image.album.id != 0) value.put(GalleryDatabaseContract.ImageEntry.COLUMN_NAME_ALBUM_ID, image.album.id);
            if (image.format != null) value.put(GalleryDatabaseContract.ImageEntry.COLUMN_NAME_FORMAT, image.format);
            if (image.owner != null) value.put(GalleryDatabaseContract.ImageEntry.COLUMN_NAME_OWNER, image.owner);
            if (image.albumName != null) value.put(GalleryDatabaseContract.ImageEntry.COLUMN_NAME_ALBUM_NAME, image.albumName);

            value.put(GalleryDatabaseContract.ImageEntry.COLUMN_NAME_ORDER, i.get());

            long row = galleryWritable.insertWithOnConflict(GalleryDatabaseContract.ImageEntry.TABLE_NAME, null, value, SQLiteDatabase.CONFLICT_IGNORE);
            if (insertOrUpdate && row == -1) galleryWritable.update(GalleryDatabaseContract.ImageEntry.TABLE_NAME, value, GalleryDatabaseContract.ImageEntry.COLUMN_NAME_ID + "=" + image.id, null);

            publishProgress(i.incrementAndGet(), j);
        }

        galleryWritable.setTransactionSuccessful();
        galleryWritable.endTransaction();

        insertsRunning --;
        if (insertsRunning == 0) galleryWritable.close();
    }

    private void insertAllAlbums(List<Album> albums, boolean insertOrUpdate) {
        insertsRunning ++;
        SQLiteDatabase galleryWritable = databaseHelper.getWritableDatabase();

        galleryWritable.beginTransaction();

        AtomicInteger i = new AtomicInteger();
        int j = albums.size();

        for (Album album : albums) {
            ContentValues value = new ContentValues();
            value.put(GalleryDatabaseContract.AlbumEntry.COLUMN_NAME_ID, album.id);
            if (album.name != null) value.put(GalleryDatabaseContract.AlbumEntry.COLUMN_NAME_NAME, album.name);
            if (album.owner != null) value.put(GalleryDatabaseContract.AlbumEntry.COLUMN_NAME_CREATOR_NAME, album.owner);
            if (album.creationDate != null) value.put(GalleryDatabaseContract.AlbumEntry.COLUMN_NAME_CREATION_DATE, album.creationDate);
            if (album.dates != null) value.put(GalleryDatabaseContract.AlbumEntry.COLUMN_NAME_DATES, album.dates.toString());
            if (album.categories != null) value.put(GalleryDatabaseContract.AlbumEntry.COLUMN_NAME_CATEGORIES, album.categories.toString());
            if (album.persons != null) value.put(GalleryDatabaseContract.AlbumEntry.COLUMN_NAME_PERSONS, album.persons.toString());

            long row = galleryWritable.insertWithOnConflict(GalleryDatabaseContract.AlbumEntry.TABLE_NAME, null, value, SQLiteDatabase.CONFLICT_IGNORE);
            if (insertOrUpdate && row == -1) galleryWritable.update(GalleryDatabaseContract.AlbumEntry.TABLE_NAME, value, GalleryDatabaseContract.ImageEntry.COLUMN_NAME_ID + "=" + album.id, null);

            publishProgress(i.incrementAndGet(), j);
        }

        galleryWritable.setTransactionSuccessful();
        galleryWritable.endTransaction();

        insertsRunning --;
        if (insertsRunning == 0) galleryWritable.close();
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (!success && (mode == Mode.QUERY) && (receiver != null)) receiver.onDatabaseError();
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (mode == Mode.INSERT_ALL_IMAGES && (receiver != null)) receiver.onInsertAllUpdate(values[0], values[1]);
        else if (mode == Mode.INSERT_ALL_ALBUMS && (receiver != null)) receiver.onInsertAllUpdate(values[0], values[1]);
    }

    enum Mode {
        QUERY, INSERT_ALL_IMAGES, INSERT_ALL_ALBUMS
    }
}
