package eu.jonahbauer.qed.util;

import eu.jonahbauer.qed.BuildConfig;
import eu.jonahbauer.qed.MockSharedPreferences;
import eu.jonahbauer.qed.model.Release;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.when;

public class UpdateCheckerTest {
    private static final Release V1_0_0 = new Release(
            "v1.0.0",
            "https://github.com/jbb01/qed/releases/v1.0.0",
            Instant.MIN,
            null,
            false
    );
    private static final Release V999_999_999 = new Release(
            "v999.999.999",
            "https://github.com/jbb01/qed/releases/v999.999.999",
            Instant.MAX,
            null,
            false
    );

    @Before
    public void init() {
        Assume.assumeTrue(BuildConfig.UPDATE_CHECK);
        MockSharedPreferences.init();
    }

    @Test
    public void testCallReturnsNullWhenNotEnabled() throws Exception {
        Preferences.getGeneral().setUpdateCheckEnabled(false);

        UpdateChecker checker = Mockito.mock(UpdateChecker.class);
        when(checker.getLatestRelease(anyBoolean())).thenReturn(V999_999_999);
        doCallRealMethod().when(checker).call();

        Release release = checker.call();
        assertNull(release);
    }

    @Test
    public void testCallReturnsNullWhenReleaseIsOld() throws Exception {
        Preferences.getGeneral().setUpdateCheckEnabled(true);
        Preferences.getGeneral().setUpdateCheckIncludesPrereleases(false);

        UpdateChecker checker = Mockito.mock(UpdateChecker.class);
        when(checker.getLatestRelease(anyBoolean())).thenReturn(V1_0_0);
        doCallRealMethod().when(checker).call();

        Release release = checker.call();
        assertNull(release);
    }

    @Test
    public void testCallReturnsNullWhenReleaseIsIgnored() throws Exception {
        Preferences.getGeneral().setUpdateCheckEnabled(true);
        Preferences.getGeneral().setUpdateCheckIncludesPrereleases(false);
        Preferences.getGeneral().setUpdateCheckDontSuggest(Set.of(V999_999_999.getVersion()));

        UpdateChecker checker = Mockito.mock(UpdateChecker.class);
        when(checker.getLatestRelease(anyBoolean())).thenReturn(V999_999_999);
        doCallRealMethod().when(checker).call();

        Release release = checker.call();
        assertNull(release);
    }

    @Test
    public void testCallReturnsReleaseWhenMoreRecent() throws Exception {
        Preferences.getGeneral().setUpdateCheckEnabled(true);
        Preferences.getGeneral().setUpdateCheckIncludesPrereleases(false);

        UpdateChecker checker = Mockito.mock(UpdateChecker.class);
        when(checker.getLatestRelease(false)).thenReturn(V999_999_999);
        doCallRealMethod().when(checker).call();

        Release release = checker.call();
        assertEquals(V999_999_999, release);
    }

    @Test
    public void testCallReturnsPrereleaseWhenMoreRecent() throws Exception {
        Preferences.getGeneral().setUpdateCheckEnabled(true);
        Preferences.getGeneral().setUpdateCheckIncludesPrereleases(true);

        UpdateChecker checker = Mockito.mock(UpdateChecker.class);
        when(checker.getLatestRelease(true)).thenReturn(V999_999_999);
        doCallRealMethod().when(checker).call();

        Release release = checker.call();
        assertEquals(V999_999_999, release);
    }
}
