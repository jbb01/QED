package com.jonahbauer.qed.model;

import android.os.Parcel;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ReleaseTest {
    private static final Release V1_2_3 = create("v2.5");
    private static final Release V2_5 = create("v2.5");
    private static final Release V2_5_0 = create("v2.5.0");
    private static final Release V2_5_1_ALPHA = create("v2.5.1-alpha");
    private static final Release V2_5_1_ALPHA_2 = create("v2.5.1-alpha2");
    private static final Release V2_5_1_BETA = create("v2.5.1-beta");
    private static final Release V2_5_1_BETA3 = create("v2.5.1-beta3");
    private static final Release V2_5_1_RC = create("v2.5.1-rc");
    private static final Release V2_5_1 = create("v2.5.1");
    private static final Release V2_5_1_NEW = create("v2.5.1", Instant.MAX);

    private static Release create(String version) {
        return create(version, Instant.MIN);
    }

    private static Release create(String version, Instant timestamp) {
        return new Release(version, "", timestamp, null, false);
    }

    @Test
    public void testCompareTo() {
        var list = List.of(
                V1_2_3,
                V2_5,
                V2_5_0,
                V2_5_1_ALPHA,
                V2_5_1_ALPHA_2,
                V2_5_1_BETA,
                V2_5_1_BETA3,
                V2_5_1_RC,
                V2_5_1,
                V2_5_1_NEW
        );
        var sorted = new ArrayList<>(list);
        Collections.shuffle(sorted, new Random(0));
        sorted.sort(Comparator.naturalOrder());

        assertEquals(list, sorted);
    }

    @Test
    public void testParse() throws JSONException {
        var json = new JSONObject();
        json.put("tag_name", "v2.5.0");
        json.put("html_url", "https://github.com/jbb01/qed/releases/v2.5.0");
        json.put("created_at", "2007-12-03T10:15:30.00Z");
        json.put("body", "Notes");
        json.put("prerelease", false);

        var release = Release.parse(json);
        assertEquals("v2.5.0", release.getVersion());
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
