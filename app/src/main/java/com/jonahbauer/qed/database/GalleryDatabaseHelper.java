package com.jonahbauer.qed.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static com.jonahbauer.qed.database.GalleryDatabaseContract.AlbumEntry;
import static com.jonahbauer.qed.database.GalleryDatabaseContract.ImageEntry;

class GalleryDatabaseHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 10;
    private static final String DATABASE_NAME = "gallery.db";

    private static final String SQL_CREATE_ENTRIES_ALBUM = "CREATE TABLE " + AlbumEntry.TABLE_NAME + " (" +
                    AlbumEntry.COLUMN_NAME_ID + " INTEGER PRIMARY KEY," +
                    AlbumEntry.COLUMN_NAME_NAME + " TEXT," +
                    AlbumEntry.COLUMN_NAME_CREATOR_NAME + " TEXT," +
                    AlbumEntry.COLUMN_NAME_CREATION_DATE + " TEXT," +
                    AlbumEntry.COLUMN_NAME_CATEGORIES + " BLOB," +
                    AlbumEntry.COLUMN_NAME_PERSONS + " BLOB," +
                    AlbumEntry.COLUMN_NAME_DATES + " BLOB," +
                    AlbumEntry.COLUMN_NAME_IMAGE_LIST_DOWNLOADED + " INTEGER)";

    private static final String SQL_CREATE_ENTRIES_IMAGES = "CREATE TABLE " + ImageEntry.TABLE_NAME + " (" +
            ImageEntry.COLUMN_NAME_ID + " INTEGER PRIMARY KEY," +
            ImageEntry.COLUMN_NAME_NAME + " TEXT," +
            ImageEntry.COLUMN_NAME_OWNER + " TEXT," +
            ImageEntry.COLUMN_NAME_PATH + " TEXT," +
            ImageEntry.COLUMN_NAME_THUMBNAIL_PATH + " TEXT," +
            ImageEntry.COLUMN_NAME_FORMAT + " TEXT," +
            ImageEntry.COLUMN_NAME_ALBUM_NAME + " TEXT," +
            ImageEntry.COLUMN_NAME_UPLOAD_DATE + " INTEGER," +
            ImageEntry.COLUMN_NAME_CREATION_DATE + " INTEGER," +
            ImageEntry.COLUMN_NAME_IS_ORIGINAL + " INTEGER," +
            ImageEntry.COLUMN_NAME_ALBUM_ID + " INTEGER," +
            ImageEntry.COLUMN_NAME_ORDER + " INTEGER," +
            "FOREIGN KEY(" + ImageEntry.COLUMN_NAME_ALBUM_ID + ") REFERENCES " + AlbumEntry.TABLE_NAME + "(" + AlbumEntry.COLUMN_NAME_ID + "))";

    private static final String SQL_DELETE_ENTRIES_ALBUM =
            "DROP TABLE IF EXISTS " + AlbumEntry.TABLE_NAME;
    private static final String SQL_DELETE_ENTRIES_IMAGES =
            "DROP TABLE IF EXISTS " + ImageEntry.TABLE_NAME;

    GalleryDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES_ALBUM);
        db.execSQL(SQL_CREATE_ENTRIES_IMAGES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES_ALBUM);
        db.execSQL(SQL_DELETE_ENTRIES_IMAGES);
        onCreate(db);
    }
}

