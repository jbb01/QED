package com.jonahbauer.qed.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.jonahbauer.qed.model.room.Converters;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Data;

@Data
@Entity
@TypeConverters(Converters.class)
public class Album implements Parcelable {
    public static final String CATEGORY_ETC = "Sonstige";

    @PrimaryKey
    private final long id;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "owner")
    private String owner;

    @ColumnInfo(name = "creation_date")
    private String creationDate;

    @ColumnInfo(name = "private")
    private boolean private_;

    @ColumnInfo(name = "persons")
    private List<Person> persons = new ArrayList<>();

    @ColumnInfo(name = "dates")
    private List<LocalDate> dates = new ArrayList<>();

    @ColumnInfo(name = "categories")
    private List<String> categories = new ArrayList<>();

    @Ignore
    private final List<Image> images = new ArrayList<>();

    /**
     * This flag indicates whether the image list for this album was downloaded and stored in the
     * database. If this flag is not set the album cannot be viewed in offline mode.
     */
    @ColumnInfo(name = "image_list_downloaded")
    private boolean imageListDownloaded;

    /**
     * This flag indicates whether the album has been completely loaded from the website.
     */
    @Ignore
    private boolean loaded;

    @NonNull
    @Override
    public String toString() {
        LinkedList<String> entries = new LinkedList<>();
        if (id != -1) entries.add("\"id\":" + id);
        if (name != null) entries.add("\"name\":\"" + name + "\"");
        if (owner != null) entries.add("\"owner\":\"" + owner + "\"");
        if (creationDate != null) entries.add("\"creationDate\":\"" + creationDate + "\"");
        entries.add("\"private\": " + private_);
        entries.add("\"imageListDownloaded\": " + imageListDownloaded);
        if (!persons.isEmpty()) entries.add("\"persons\":" + persons);
        if (!dates.isEmpty()) entries.add("\"dates\":" + dates);
        if (!categories.isEmpty()) entries.add("\"categories\":" + categories);
        return entries.stream().collect(Collectors.joining(", ", "{", "}"));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(name);
        dest.writeString(owner);
        dest.writeString(creationDate);
        dest.writeInt(private_ ? 1 : 0);
        dest.writeTypedList(persons);
        dest.writeInt(dates.size());
        for (LocalDate date : dates) {
            dest.writeSerializable(date);
        }
        dest.writeStringList(categories);
        dest.writeTypedList(images);
        dest.writeInt(imageListDownloaded ? 1 : 0);
    }

    public static final Parcelable.Creator<Album> CREATOR = new Parcelable.Creator<>() {
        @NonNull
        @Override
        public Album createFromParcel(@NonNull Parcel source) {
            Album album = new Album(source.readLong());
            album.name = source.readString();
            album.owner = source.readString();
            album.creationDate = source.readString();
            album.private_ = source.readInt() != 0;
            source.readTypedList(album.persons, Person.CREATOR);

            int dateSize = source.readInt();
            for (int i = 0; i < dateSize; i++) {
                album.dates.add((LocalDate) source.readSerializable());
            }

            source.readStringList(album.categories);
            source.readTypedList(album.images, Image.CREATOR);
            album.imageListDownloaded = source.readInt() != 0;
            return album;
        }

        @Override
        public Album[] newArray(int size) {
            return new Album[size];
        }
    };
}
