package com.jonahbauer.qed.model;

import com.jonahbauer.qed.model.Release.Version;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class ReleaseVersionTest {
    @Test
    public void testParseSimple() {
        var version = new Version("v2.5.0");
        assertEquals(2, version.getMajor());
        assertEquals(5, version.getMinor());
        assertEquals(0, version.getPatch());
        assertEquals(List.of(), version.getPrerelease());
        assertEquals(List.of(), version.getBuild());
    }

    @Test
    public void testParsePrerelease() {
        var version = new Version("v3.1.4-foo.bar.foo-bar.01baz");
        assertEquals(3, version.getMajor());
        assertEquals(1, version.getMinor());
        assertEquals(4, version.getPatch());
        assertEquals(List.of("foo", "bar", "foo-bar", "01baz"), version.getPrerelease());
        assertEquals(List.of(), version.getBuild());

        assertThrows(IllegalArgumentException.class, () -> {
            new Version("v3.1.4-01");
        });
    }

    @Test
    public void testParseBuild() {
        var version = new Version("v3.1.4+foo.bar.foo-bar.01");
        assertEquals(3, version.getMajor());
        assertEquals(1, version.getMinor());
        assertEquals(4, version.getPatch());
        assertEquals(List.of(), version.getPrerelease());
        assertEquals(List.of("foo", "bar", "foo-bar", "01"), version.getBuild());
    }

    @Test
    public void testSortSimple() {
        var v1_0_0 = new Version(1, 0, 0);
        var v2_0_0 = new Version(2, 0, 0);
        var v2_1_0 = new Version(2, 1, 0);
        var v2_1_1 = new Version(2, 1, 1);

        var list = Arrays.asList(v2_0_0, v2_1_1, v2_1_0, v1_0_0);
        Collections.sort(list);

        assertEquals(List.of(v1_0_0, v2_0_0, v2_1_0, v2_1_1), list);
    }

    @Test
    public void testSortPrereleaseRelease() {
        var v1_0_0_alpha = new Version(1, 0, 0, List.of("alpha"));
        var v1_0_0 = new Version(1, 0, 0);

        var list = Arrays.asList(v1_0_0, v1_0_0_alpha);
        Collections.sort(list);

        assertEquals(List.of(v1_0_0_alpha, v1_0_0), list);
    }

    @Test
    public void testSortPrereleases() {
        var v1_0_0_alpha = new Version(1, 0, 0, List.of("alpha"));
        var v1_0_0_alpha_1 = new Version(1, 0, 0, List.of("alpha", "1"));
        var v1_0_0_alpha_beta = new Version(1, 0, 0, List.of("alpha", "beta"));
        var v1_0_0_beta = new Version(1, 0, 0, List.of("beta"));
        var v1_0_0_beta_2 = new Version(1, 0, 0, List.of("beta", "2"));
        var v1_0_0_beta_11 = new Version(1, 0, 0, List.of("beta", "11"));
        var v1_0_0_rc_1 = new Version(1, 0, 0, List.of("rc", "1"));
        var v1_0_0 = new Version(1, 0, 0);

        var list = Arrays.asList(v1_0_0_beta,
                                 v1_0_0_rc_1,
                                 v1_0_0_alpha_beta,
                                 v1_0_0_beta_11,
                                 v1_0_0_alpha_1,
                                 v1_0_0_alpha,
                                 v1_0_0,
                                 v1_0_0_beta_2
        );
        Collections.sort(list);

        assertEquals(List.of(v1_0_0_alpha,
                             v1_0_0_alpha_1,
                             v1_0_0_alpha_beta,
                             v1_0_0_beta,
                             v1_0_0_beta_2,
                             v1_0_0_beta_11,
                             v1_0_0_rc_1,
                             v1_0_0
        ), list);
    }
}
