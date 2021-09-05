package com.jonahbauer.qed.model;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.jonahbauer.qed.model.room.Converters;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Data;

@Data
@Entity
@TypeConverters(Converters.class)
public class Image implements Parcelable {
    public static final Set<String> VIDEO_FILE_EXTENSIONS;
    public static final Set<String> AUDIO_FILE_EXTENSIONS;

    public static final String DATA_KEY_ALBUM = "album";
    public static final String DATA_KEY_OWNER = "owner";
    public static final String DATA_KEY_FORMAT = "format";
    public static final String DATA_KEY_UPLOAD_DATE = "uploaded";
    public static final String DATA_KEY_CREATION_DATE = "created";
    public static final String DATA_KEY_ORIENTATION = "orientation";
    public static final String DATA_KEY_MANUFACTURER = "manufacturer";
    public static final String DATA_KEY_MODEL = "model";
    public static final String DATA_KEY_FOCAL_LENGTH = "focal length";
    public static final String DATA_KEY_FOCAL_RATIO = "focal ration";
    public static final String DATA_KEY_EXPOSURE_TIME = "exposure time";
    public static final String DATA_KEY_ISO = "iso";
    public static final String DATA_KEY_POSITION = "position";
    public static final String DATA_KEY_FLASH = "flash";
    public static final String DATA_KEY_VISITS = "visits";
    public static final String DATA_KEY_RESOLUTION = "resolution";

    static {
        HashSet<String> videoFileExtensions = new HashSet<>();
        videoFileExtensions.add("mp4");
        videoFileExtensions.add("mkv");
        videoFileExtensions.add("flv");
        videoFileExtensions.add("mov");
        videoFileExtensions.add("avi");
        videoFileExtensions.add("wmv");
        videoFileExtensions.add("mpeg");
        videoFileExtensions.add("webm");
        VIDEO_FILE_EXTENSIONS = Collections.unmodifiableSet(videoFileExtensions);

        HashSet<String> audioFileExtensions = new HashSet<>();
        audioFileExtensions.add("wav");
        audioFileExtensions.add("mp3");
        audioFileExtensions.add("ogg");
        audioFileExtensions.add("m4a");
        audioFileExtensions.add("flac");
        audioFileExtensions.add("opus");
        AUDIO_FILE_EXTENSIONS = Collections.unmodifiableSet(audioFileExtensions);
    }

    @PrimaryKey
    private final long id;

    @ColumnInfo(name = "album_id")
    private long albumId;

    @ColumnInfo(name = "order")
    private long order;

    @ColumnInfo(name = "album_name")
    private String albumName;

    @ColumnInfo(name = "format")
    private String format;

    @ColumnInfo(name = "path")
    private String path;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "owner")
    private String owner;

    @ColumnInfo(name = "upload_date")
    private Date uploadDate;

    @ColumnInfo(name = "creation_date")
    private Date creationDate;

    @ColumnInfo(name = "original")
    private boolean original;

    @ColumnInfo(name = "thumbnail")
    private Bitmap thumbnail;

    @ColumnInfo(name = "data")
    private Map<String, String> data = new HashMap<>();

    @ColumnInfo(name = "loaded")
    private boolean loaded;

    @Ignore
    private boolean databaseLoaded;

    public void set(Image image) {
        this.setAlbumId(image.getAlbumId());
        this.setAlbumName(image.getAlbumName());
        this.setFormat(image.getFormat());
        this.setPath(image.getPath());
        this.setName(image.getName());
        this.setOwner(image.getOwner());
        this.setUploadDate(image.getUploadDate());
        this.setCreationDate(image.getCreationDate());
        this.setOriginal(image.isOriginal());
        this.setLoaded(image.isLoaded());
        this.setThumbnail(image.getThumbnail());
        this.setData(image.getData());
    }

    @Override
    @NonNull
    public String toString() {
        LinkedList<String> entries = new LinkedList<>();
        if (id != -1) entries.add("\"id\":" + id);
        if (format != null) entries.add("\"format\":\"" + format + "\"");
        if (path != null) entries.add("\"path\":\"" + path + "\"");
        if (name != null) entries.add("\"name\":\"" + name + "\"");
        if (owner != null) entries.add("\"owner\":\"" + owner + "\"");
        if (uploadDate != null) entries.add("\"uploadDate\":" + uploadDate.getTime());
        if (creationDate != null) entries.add("\"creationDate\":" + creationDate.getTime());
        entries.add("\"original\":" + original);
        entries.add("\"loaded\":" + loaded);
        if (!data.isEmpty()) entries.add(data.entrySet().stream().map(e -> "\"" + e.getKey() + "\":\"" + e.getValue() + "\"").collect(Collectors.joining(",")));
        if (albumName != null) entries.add("\"albumName\":\"" + albumName + "\"");
        if (albumId != -1) entries.add("\"albumId\":" + albumId);
        return entries.stream().collect(Collectors.joining(", ", "{", "}"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Image image = (Image) o;
        return id == image.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(albumName);
        dest.writeString(format);
        dest.writeString(path);
        dest.writeString(owner);
        dest.writeSerializable(uploadDate);
        dest.writeSerializable(creationDate);
        dest.writeInt((original ? 1 : 0));
        dest.writeInt((loaded ? 1 : 0));
        dest.writeMap(data);
    }

    public static final Parcelable.Creator<Image> CREATOR = new Parcelable.Creator<Image>() {
        @NonNull
        @Override
        public Image createFromParcel(@NonNull Parcel source) {
            Image image = new Image(source.readLong());
            image.albumName = source.readString();
            image.format = source.readString();
            image.path = source.readString();
            image.owner = source.readString();
            image.uploadDate = (Date) source.readSerializable();
            image.creationDate = (Date) source.readSerializable();
            image.original = source.readInt() != 0;
            image.loaded = source.readInt() != 0;

            source.readMap(image.data, Image.class.getClassLoader());

            return image;
        }

        @Override
        public Image[] newArray(int size) {
            return new Image[size];
        }
    };
}
