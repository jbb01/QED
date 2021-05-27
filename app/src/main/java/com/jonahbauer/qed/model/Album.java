package com.jonahbauer.qed.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Data;

@Data
public class Album implements Parcelable {
    public static final String CATEGORY_ETC = "Sonstige";

    private final long id;
    private String name;
    private String owner;
    private String creationDate;
    private final List<Person> persons = new ArrayList<>();
    private final List<Date> dates = new ArrayList<>();
    private final List<String> categories = new ArrayList<>();
    private final List<Image> images = new ArrayList<>();
    private boolean imageListDownloaded;

    @NonNull
    @Override
    public String toString() {
        LinkedList<String> entries = new LinkedList<>();
        if (id != -1) entries.add("\"id\":" + id);
        if (name != null) entries.add("\"name\":\"" + name + "\"");
        if (owner != null) entries.add("\"owner\":\"" + owner + "\"");
        if (creationDate != null) entries.add("\"creationDate\":" + creationDate);
        if (!persons.isEmpty()) entries.add("\"persons\":" + persons);
        if (!dates.isEmpty()) entries.add("\"dates\":" + dates);
        if (!categories.isEmpty()) entries.add("\"categories\":" + categories);
        if (!images.isEmpty()) entries.add("\"images\":" + images);
        entries.add("\"imageListDownloaded\":" + imageListDownloaded);
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
        dest.writeTypedList(persons);
        dest.writeInt(dates.size());
        for (Date date : dates) {
            dest.writeLong(date.getTime());
        }
        dest.writeStringList(categories);
        dest.writeTypedList(images);
        dest.writeInt(imageListDownloaded ? 1 : 0);
    }

    public static final Parcelable.Creator<Album> CREATOR = new Parcelable.Creator<Album>() {
        @NonNull
        @Override
        public Album createFromParcel(@NonNull Parcel source) {
            Album album = new Album(source.readLong());
            album.name = source.readString();
            album.owner = source.readString();
            album.creationDate = source.readString();
            source.readTypedList(album.persons, Person.CREATOR);

            int dateSize = source.readInt();
            for (int i = 0; i < dateSize; i++) {
                album.dates.add(new Date(source.readLong()));
            }

            source.readStringList(album.categories);
            source.readTypedList(album.images, Image.CREATOR);
            album.imageListDownloaded = source.readInt() == 1;
            return album;
        }

        @Override
        public Album[] newArray(int size) {
            return new Album[size];
        }
    };
}
