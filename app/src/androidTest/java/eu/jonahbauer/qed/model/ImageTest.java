package eu.jonahbauer.qed.model;

import android.os.Parcel;
import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.assertEquals;

public class ImageTest {

    @Test
    public void testParcelableConsistentWithEquals() {
        var image = dummyImage();

        var parcel = Parcel.obtain();
        image.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);

        var out = Image.CREATOR.createFromParcel(parcel);
        assertEquals(image, out);
    }

    private static Image dummyImage() {
        var image = new Image(1);
        image.setAlbumId(1);
        image.setOrder(0);
        image.setAlbumName("Musteralbum");
        image.setFormat("image/jpeg");
        image.setPath("/foo/bar");
        image.setName("Musterbild");
        image.setOwner("Max Mustermann");
        image.setUploadTime(Instant.now());
        image.setCreationTime(Instant.now());
        image.setThumbnail(null);
        image.getData().put(Image.DATA_KEY_MANUFACTURER, "OnePlus");
        image.getData().put(Image.DATA_KEY_VISITS, "100");
        image.getData().put(Image.DATA_KEY_RESOLUTION, "1920x1080");
        image.setLoaded(true);
        image.setDatabaseLoaded(true);

        return image;
    }
}
