package com.jonahbauer.qed.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.jonahbauer.qed.model.parcel.ParcelExtensions;
import com.jonahbauer.qed.model.room.Converters;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Builder;
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

    @ColumnInfo(name = "upload_dates")
    private List<LocalDate> uploadDates = new ArrayList<>();

    @ColumnInfo(name = "categories")
    private List<String> categories = new ArrayList<>();

    @Ignore
    private final List<Image> images = new ArrayList<>();

    /**
     * This flag indicates whether the image list for this album was downloaded and stored in the
     * database. If this flag is not set the album cannot be viewed in offline mode.
     */
    @ColumnInfo(name = "image_list_downloaded")
    private Instant loaded;

    @NonNull
    @Override
    public String toString() {
        LinkedList<String> entries = new LinkedList<>();
        if (id != -1) entries.add("\"id\":" + id);
        if (name != null) entries.add("\"name\":\"" + name + "\"");
        if (owner != null) entries.add("\"owner\":\"" + owner + "\"");
        if (creationDate != null) entries.add("\"creationDate\":\"" + creationDate + "\"");
        entries.add("\"private\": " + private_);
        entries.add("\"loaded\": " + loaded);
        if (!persons.isEmpty()) entries.add("\"persons\":" + persons);
        if (!dates.isEmpty()) entries.add("\"dates\":" + dates);
        if (!uploadDates.isEmpty()) entries.add("\"upload_dates\":" + uploadDates);
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
        ParcelExtensions.writeBoolean(dest, private_);
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

    public static final Parcelable.Creator<Album> CREATOR = new Parcelable.Creator<>() {
        @NonNull
        @Override
        public Album createFromParcel(@NonNull Parcel source) {
            Album album = new Album(source.readLong());
            album.name = source.readString();
            album.owner = source.readString();
            album.creationDate = source.readString();
            album.private_ = ParcelExtensions.readBoolean(source);
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
        }

        @Override
        public Album[] newArray(int size) {
            return new Album[size];
        }
    };

    @Data
    @Builder(builderClassName = "Builder", setterPrefix = "set")
    public static class Filter {
        @Nullable
        private final LocalDate day;
        @Nullable
        private final LocalDate upload;
        @Nullable
        private final Person owner;
        @Nullable
        private final String category;

        @NonNull
        public String toString() {
            String out = "";
            if (day != null) {
                out += "&byday=" + DateTimeFormatter.ISO_LOCAL_DATE.format(day);
            }
            if (upload != null) {
                out += "&byupload=" + DateTimeFormatter.ISO_LOCAL_DATE.format(upload);
            }
            if (owner != null) {
                out += "&byowner=" + owner.getId();
            }
            if (category != null) {
                out += "&bycategory=";
                if (!CATEGORY_ETC.equals(category)) {
                    try {
                        out += URLEncoder.encode(category, "UTF-8");
                    } catch (UnsupportedEncodingException ignored) {}
                }
            }
            return out;
        }

        public boolean isEmpty() {
            return day == null && upload == null && owner == null && category == null;
        }
    }
}
