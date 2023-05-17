package eu.jonahbauer.qed.model;

import android.os.Parcel;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.Assert.*;

public class ReleaseTest {
    private static final Release V2_5_1_BETA = new Release("v2.5.1-beta", Instant.EPOCH);

    @Test
    public void testParse() throws JSONException {
        var json = new JSONObject();
        json.put("tag_name", "v2.5.0");
        json.put("html_url", "https://github.com/jbb01/qed/releases/v2.5.0");
        json.put("created_at", "2007-12-03T10:15:30.00Z");
        json.put("body", "Notes");
        json.put("prerelease", false);

        var release = Release.parse(json);
        assertEquals(new Release.Version(2, 5, 0), release.getVersion());
        assertEquals("https://github.com/jbb01/qed/releases/v2.5.0", release.getUrl());
        assertEquals(
                Instant.from(OffsetDateTime.of(2007, 12, 3, 10, 15, 30, 0, ZoneOffset.UTC)),
                release.getCreatedAt()
        );
        assertEquals("Notes", release.getNotes());
        assertFalse(release.isPrerelease());
    }

    @Test
    public void testParcelableConsistentWithEquals() {
        var release = V2_5_1_BETA;
        var copy = cloneWithParcelable(release);
        assertEquals(release, copy);
    }

    @Test
    public void testParcelableContainsAllNonTransientFields() throws IllegalAccessException {
        var release = V2_5_1_BETA;
        var copy = cloneWithParcelable(release);

        var fields = Release.class.getDeclaredFields();
        for (var field : fields) {
            field.setAccessible(true);
            if ((field.getModifiers() & Modifier.STATIC) != 0) continue;
            if ((field.getModifiers() & Modifier.TRANSIENT) != 0) continue;
            assertEquals(field.getName(), field.get(release), field.get(copy));
        }
    }

    private Release cloneWithParcelable(Release release) {
        var parcel = Parcel.obtain();
        try {
            release.writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            return Release.CREATOR.createFromParcel(parcel);
        } finally {
            parcel.recycle();
        }
    }
}
