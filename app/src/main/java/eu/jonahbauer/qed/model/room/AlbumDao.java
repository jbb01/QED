package eu.jonahbauer.qed.model.room;

import android.graphics.Bitmap;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.TypeConverters;

import eu.jonahbauer.qed.model.Album;
import eu.jonahbauer.qed.model.Image;

import java.util.Collection;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
@TypeConverters({Converters.class})
public interface AlbumDao {
    @Query("SELECT * FROM album ORDER BY id DESC")
    Single<List<Album>> getAll();


    @Query("SELECT * FROM album WHERE id = :id")
    Single<Album> findById(long id);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Completable insertAlbums(Collection<Album> albums);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertOrUpdateAlbum(Album album);


    @Query("SELECT * FROM image WHERE album_id = :id ORDER BY `order`")
    Single<List<Image>> findImagesByAlbum(long id);

    @Query("SELECT * FROM image WHERE id = :id")
    Single<Image> findImageById(long id);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Completable insertImages(Collection<Image> images);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertOrUpdateImage(Image image);

    @Query("UPDATE image SET thumbnail = :thumbnail WHERE id = :id")
    Completable insertThumbnail(long id, Bitmap thumbnail);

    @Query("UPDATE image SET path = :path, original = :original, format = :format WHERE id = :id")
    Completable insertImagePath(long id, String path, String format, boolean original);


    @Query("DELETE FROM image")
    Completable clearImages();

    @Query("DELETE FROM album")
    Completable clearAlbums();

    @Query("UPDATE image SET thumbnail = null")
    Completable clearThumbnails();
}
