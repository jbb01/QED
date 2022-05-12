package com.jonahbauer.qed.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.jonahbauer.qed.model.parcel.ParcelExtensions;
import com.jonahbauer.qed.model.room.Converters;

import java.net.URLDecoder;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import com.jonahbauer.qed.model.parcel.LambdaCreator;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

@Data
@Entity
@EqualsAndHashCode(of = "id")
@TypeConverters(Converters.class)
public class Album implements Parcelable {
    public static final long NO_ID = Long.MIN_VALUE;
    public static final String CATEGORY_ETC = "Sonstige";

    /**
     * The album id.
     */
    @PrimaryKey
    private final long id;

    /**
     * The album title.
     */
    @ColumnInfo(name = "name")
    private String name;

    /**
     * The username of the album owner.
     */
    @ColumnInfo(name = "owner")
    private String owner;

    /**
     * The album creation date.
     */
    @ColumnInfo(name = "creation_date")
    private String creationDate;

    /**
     * Whether this is a private album.
     */
    @ColumnInfo(name = "private")
    @Accessors(prefix = "_")
    private boolean _private;

    /**
     * A list of persons who contributed to this album. Typically only contains {@linkplain Person#getId() id}
     * and {@linkplain Person#getUsername() username}.
     */
    @ColumnInfo(name = "persons")
    private List<Person> persons = new ArrayList<>();

    /**
     * A list of dates on which images in this album were taken.
     */
    @ColumnInfo(name = "dates")
    private List<LocalDate> dates = new ArrayList<>();

    /**
     * A list of dates on which images in this album were uploaded.
     */
    @ColumnInfo(name = "upload_dates")
    private List<LocalDate> uploadDates = new ArrayList<>();

    /**
     * A list of url-encoded categories.
     * @see #decodeCategory(String)
     */
    @ColumnInfo(name = "categories")
    private List<String> categories = new ArrayList<>();

    /**
     * A list of images in this album.
     */
    @Ignore
    private final List<Image> images = new ArrayList<>();

    /**
     * This flag indicates whether the image list for this album was downloaded and stored in the
     * database. If this flag is not set the album cannot be viewed in offline mode.
     */
    @ColumnInfo(name = "image_list_downloaded")
    private Instant loaded;

    /**
     * Copies all database-persisted properties (except id) from the given album to this album.
     * @implNote This method only performs a shallow copy.
     */
    public void set(@NonNull Album other) {
        this.name = other.name;
        this.owner = other.owner;
        this.creationDate = other.creationDate;
        this._private = other._private;
        this.persons = other.persons;
        this.dates = other.dates;
        this.uploadDates = other.uploadDates;
        this.categories = other.categories;
        this.loaded = other.loaded;
    }

    @NonNull
    @Override
    public String toString() {
        var joiner = new StringJoiner(", ", "{", "}");
        if (id != NO_ID) joiner.add("\"id\":" + id);
        if (name != null) joiner.add("\"name\":\"" + name + "\"");
        if (owner != null) joiner.add("\"owner\":\"" + owner + "\"");
        if (creationDate != null) joiner.add("\"creationDate\":\"" + creationDate + "\"");
        joiner.add("\"private\": " + _private);
        joiner.add("\"loaded\": " + loaded);
        if (!persons.isEmpty()) joiner.add("\"persons\":" + persons);
        if (!dates.isEmpty()) joiner.add("\"dates\":" + dates);
        if (!uploadDates.isEmpty()) joiner.add("\"upload_dates\":" + uploadDates);
        if (!categories.isEmpty()) joiner.add("\"categories\":" + categories);
        return joiner.toString();
    }

    /**
     * Decodes an url-encoded category for displaying it to the user.
     */
    @SneakyThrows
    public static String decodeCategory(String category) {
        if (category == null) return null;
        if ("".equals(category)) return Album.CATEGORY_ETC;
        return URLDecoder.decode(category, "UTF-8");
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
        ParcelExtensions.writeBoolean(dest, _private);
        dest.writeTypedList(persons);
        dest.writeInt(dates.size());
        for (LocalDate date : dates) {
            ParcelExtensions.writeLocalDate(dest, date);
        }
        dest.writeInt(uploadDates.size());
        for (LocalDate date : uploadDates) {
            ParcelExtensions.writeLocalDate(dest, date);
        }
        dest.writeStringList(categories);
        dest.writeTypedList(images);
        ParcelExtensions.writeInstant(dest, loaded);
    }

    public static final Creator<Album> CREATOR = new LambdaCreator<>(Album[]::new, source -> {
        Album album = new Album(source.readLong());
        album.name = source.readString();
        album.owner = source.readString();
        album.creationDate = source.readString();
        album._private = ParcelExtensions.readBoolean(source);
        source.readTypedList(album.persons, Person.CREATOR);

        int dateSize = source.readInt();
        for (int i = 0; i < dateSize; i++) {
            album.dates.add(ParcelExtensions.readLocalDate(source));
        }

        int uploadDateSize = source.readInt();
        for (int i = 0; i < uploadDateSize; i++) {
            album.uploadDates.add(ParcelExtensions.readLocalDate(source));
        }

        source.readStringList(album.categories);
        source.readTypedList(album.images, Image.CREATOR);

        album.loaded = ParcelExtensions.readInstant(source);
        return album;
    });
}
