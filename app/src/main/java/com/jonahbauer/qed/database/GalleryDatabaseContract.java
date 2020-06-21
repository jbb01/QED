package com.jonahbauer.qed.database;

import android.provider.BaseColumns;

final class GalleryDatabaseContract {
    private GalleryDatabaseContract() {}


    static class AlbumEntry implements BaseColumns {
        static final String TABLE_NAME = "album";
        static final String COLUMN_NAME_ID = "albumId";
        static final String COLUMN_NAME_NAME = "albumName";
        static final String COLUMN_NAME_CREATOR_NAME = "albumCreatorName";
        static final String COLUMN_NAME_CREATION_DATE = "albumCreationDate";
        static final String COLUMN_NAME_DATES = "albumDates";
        static final String COLUMN_NAME_CATEGORIES = "albumCategories";
        static final String COLUMN_NAME_PERSONS = "albumPersons";
        static final String COLUMN_NAME_IMAGE_LIST_DOWNLOADED = "albumImageListDownloaded";
    }

    static class ImageEntry implements BaseColumns {
        static final String TABLE_NAME = "images";
        static final String COLUMN_NAME_ID = "imageId";
        static final String COLUMN_NAME_NAME = "imageName";
        static final String COLUMN_NAME_OWNER = "imageOwner";
        static final String COLUMN_NAME_ALBUM_ID = "imageAlbumId";
        static final String COLUMN_NAME_ALBUM_NAME = "imageAlbumName";
        static final String COLUMN_NAME_PATH = "imagePath";
        static final String COLUMN_NAME_FORMAT = "imageFormat";
        static final String COLUMN_NAME_UPLOAD_DATE = "imageUploadDate";
        static final String COLUMN_NAME_CREATION_DATE = "imageCreationDate";
        static final String COLUMN_NAME_IS_ORIGINAL = "imageIsOriginal";
        static final String COLUMN_NAME_ORDER = "imageOrder";
        static final String COLUMN_NAME_DATA = "imageData";
    }

    static class ThumbEntry implements BaseColumns {
        static final String TABLE_NAME = "thumbs";
        static final String COLUMN_NAME_ID = "imageId";
        static final String COLUMN_NAME_THUMBNAIL = "thumbnail";
    }
}
