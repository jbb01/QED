package eu.jonahbauer.qed.model;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.model.parcel.ParcelExtensions;
import eu.jonahbauer.qed.model.room.Converters;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import eu.jonahbauer.qed.model.parcel.LambdaCreator;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@EqualsAndHashCode(of = "id")
@TypeConverters(Converters.class)
public class Image implements Parcelable {
    public static final long NO_ID = Long.MIN_VALUE;

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

    private static final Set<String> VIDEO_FILE_EXTENSIONS;
    private static final Set<String> AUDIO_FILE_EXTENSIONS;
    private static final Set<String> IMAGE_FILE_EXTENSIONS;
    private static final Map<Image.Type, Integer> THUMBNAIL_BY_TYPE;
    private static final Map<Image.Type, Integer> THUMBNAIL_ERROR_BY_TYPE;

    static {
        VIDEO_FILE_EXTENSIONS = ObjectSet.of("mp4", "mkv", "flv", "mov", "avi", "wmv", "mpeg", "webm");
        AUDIO_FILE_EXTENSIONS = ObjectSet.of("wav", "mp3", "ogg", "m4a", "flac", "opus");
        IMAGE_FILE_EXTENSIONS = ObjectSet.of("jpg", "jpeg", "png", "webp", "gif", "tif", "bmp", "ico", "svg");
    }

    static {
        var thumbnail = new EnumMap<Image.Type, Integer>(Image.Type.class);
        thumbnail.put(Image.Type.IMAGE, R.drawable.ic_gallery_image);
        thumbnail.put(Image.Type.OTHER, R.drawable.ic_gallery_other);
        thumbnail.put(Image.Type.AUDIO, R.drawable.ic_gallery_audio);
        thumbnail.put(Image.Type.VIDEO, R.drawable.ic_gallery_video);
        THUMBNAIL_BY_TYPE = Collections.unmodifiableMap(thumbnail);

        var thumbnailError = new EnumMap<Image.Type, Integer>(Image.Type.class);
        thumbnailError.put(Image.Type.IMAGE, R.drawable.ic_gallery_empty_image);
        thumbnailError.put(Image.Type.OTHER, R.drawable.ic_gallery_empty_other);
        thumbnailError.put(Image.Type.AUDIO, R.drawable.ic_gallery_empty_audio);
        thumbnailError.put(Image.Type.VIDEO, R.drawable.ic_gallery_empty_video);
        THUMBNAIL_ERROR_BY_TYPE = Collections.unmodifiableMap(thumbnailError);
    }

    public static @DrawableRes int getThumbnail(@NonNull Image.Type type, boolean success) {
        return Objects.requireNonNull((success ? THUMBNAIL_BY_TYPE : THUMBNAIL_ERROR_BY_TYPE).get(type));
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
    private Instant uploadTime;

    @ColumnInfo(name = "creation_date")
    private Instant creationTime;

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
        this.setUploadTime(image.getUploadTime());
        this.setCreationTime(image.getCreationTime());
        this.setOriginal(image.isOriginal());
        this.setLoaded(image.isLoaded());
        this.setThumbnail(image.getThumbnail());
        this.setData(image.getData());
    }

    @Override
    @NonNull
    public String toString() {
        var joiner = new StringJoiner(", ", "{", "}");
        if (id != NO_ID) joiner.add("\"id\":" + id);
        if (format != null) joiner.add("\"format\":\"" + format + "\"");
        if (path != null) joiner.add("\"path\":\"" + path + "\"");
        if (name != null) joiner.add("\"name\":\"" + name + "\"");
        if (owner != null) joiner.add("\"owner\":\"" + owner + "\"");
        if (uploadTime != null) joiner.add("\"uploadDate\":" + uploadTime.getEpochSecond());
        if (creationTime != null) joiner.add("\"creationDate\":" + creationTime.getEpochSecond());
        joiner.add("\"original\":" + original);
        joiner.add("\"loaded\":" + loaded);
        if (!data.isEmpty()) joiner.add(data.entrySet().stream().map(e -> "\"" + e.getKey() + "\":\"" + e.getValue() + "\"").collect(Collectors.joining(",")));
        if (albumName != null) joiner.add("\"albumName\":\"" + albumName + "\"");
        if (albumId != Album.NO_ID) joiner.add("\"albumId\":" + albumId);
        return joiner.toString();
    }

    public @NonNull Type getType() {
        if (getFormat() != null) {
            String format = getFormat().split("/")[0];
            if ("audio".equals(format)) {
                return Type.AUDIO;
            } else if ("video".equals(format)) {
                return Type.VIDEO;
            } else if ("image".equals(format)) {
                return Type.IMAGE;
            } else {
                return Type.OTHER;
            }
        } else if (getName() != null) {
            String suffix = getName();
            suffix = suffix.substring(suffix.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);

            if (Image.VIDEO_FILE_EXTENSIONS.contains(suffix)) {
                return Type.VIDEO;
            } else if (Image.AUDIO_FILE_EXTENSIONS.contains(suffix)) {
                return Type.AUDIO;
            } else if (Image.IMAGE_FILE_EXTENSIONS.contains(suffix)) {
                return Type.IMAGE;
            } else {
                return Type.OTHER;
            }
        }

        return Type.OTHER;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeLong(albumId);
        dest.writeLong(order);
        dest.writeString(albumName);
        dest.writeString(format);
        dest.writeString(path);
        dest.writeString(name);
        dest.writeString(owner);
        ParcelExtensions.writeInstant(dest, uploadTime);
        ParcelExtensions.writeInstant(dest, creationTime);
        ParcelExtensions.writeBoolean(dest, original);
        dest.writeMap(data);
        ParcelExtensions.writeBoolean(dest, loaded);
    }

    public static final Creator<Image> CREATOR = new LambdaCreator<>(Image[]::new, source -> {
        Image image = new Image(source.readLong());
        image.albumId = source.readLong();
        image.order = source.readLong();
        image.albumName = source.readString();
        image.format = source.readString();
        image.path = source.readString();
        image.name = source.readString();
        image.owner = source.readString();
        image.uploadTime = ParcelExtensions.readInstant(source);
        image.creationTime = ParcelExtensions.readInstant(source);
        image.original = ParcelExtensions.readBoolean(source);
        source.readMap(image.data, Image.class.getClassLoader());
        image.loaded = ParcelExtensions.readBoolean(source);
        return image;
    });

    public enum Type {
        IMAGE, VIDEO, AUDIO, OTHER
    }
}
