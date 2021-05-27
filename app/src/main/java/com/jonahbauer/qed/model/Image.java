package com.jonahbauer.qed.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

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
public class Image implements Parcelable {
    public static final Set<String> VIDEO_FILE_EXTENSIONS;
    public static final Set<String> AUDIO_FILE_EXTENSIONS;

    static {
        HashSet<String> videoFileExtensions = new HashSet<>();
        videoFileExtensions.add("mp4");
        videoFileExtensions.add("mkv");
        videoFileExtensions.add("flv");
        videoFileExtensions.add("mov");
        videoFileExtensions.add("avi");
        videoFileExtensions.add("wmv");
        videoFileExtensions.add("mpeg");
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

    private final long id;
    private transient Album album;
    private String albumName;
    private String format;
    private String path;
    private String name;
    private String owner;
    private Date uploadDate;
    private Date creationDate;
    private boolean original;
    private boolean available = true;

    private final Map<String, String> data = new HashMap<>();

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
        entries.add("\"available\":" + available);
        if (!data.isEmpty()) entries.add(data.entrySet().stream().map(e -> "\"" + e.getKey() + "\":\"" + e.getValue() + "\"").collect(Collectors.joining(",")));
        if (albumName != null) entries.add("\"albumName\":\"" + albumName + "\"");
        if (album != null) entries.add("\"album\":" + album);
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
        dest.writeLong(uploadDate != null ? uploadDate.getTime() : -1);
        dest.writeLong(creationDate != null ? creationDate.getTime() : -1);
        dest.writeInt((original ? 2 : 0) + (available ? 1 : 0));
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

            long uploadDate = source.readLong();
            if (uploadDate != -1) image.uploadDate = new Date(uploadDate);

            long creationDate = source.readLong();
            if (creationDate != -1) image.creationDate = new Date(creationDate);

            int originalAvailable = source.readInt();
            image.original = originalAvailable / 2 == 1;
            image.available = originalAvailable % 2 == 1;

            source.readMap(image.data, Image.class.getClassLoader());

            return image;
        }

        @Override
        public Image[] newArray(int size) {
            return new Image[size];
        }
    };
}
