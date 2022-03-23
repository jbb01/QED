package com.jonahbauer.qed.model;

import android.os.Parcel;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.Assert.assertEquals;

public class AlbumTest {

    @Test
    public void testParcelableConsistentWithEquals() {
        var album = dummyAlbum();

        var parcel = Parcel.obtain();
        album.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);

        var out = Album.CREATOR.createFromParcel(parcel);
        assertEquals(album, out);
    }

    private static Album dummyAlbum() {
        var album = new Album(1);
        album.setName("Musteralbum");
        album.setOwner("Max Mustermann");
        album.setCreationDate("01.01.2000");
        album.setPrivate_(false);

        var person = new Person(1);
        person.setUsername("ErikaMusterfrau");
        album.getPersons().add(person);

        album.getDates().add(LocalDate.of(2000, 1, 1));
        album.getUploadDates().add(LocalDate.of(2000, 1, 2));
        album.getCategories().add("");
        album.getCategories().add("Test");

        var image = new Image(1);
        image.setAlbumId(album.getId());
        image.setAlbumName(album.getName());
        album.getImages().add(image);

        album.setLoaded(Instant.now());

        return album;
    }
}
