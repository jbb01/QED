package eu.jonahbauer.qed.util.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.preference.PreferenceManager;
import androidx.test.annotation.UiThreadTest;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SharedPreferenceLiveDataTest {
    public static final String PREFERENCE_KEY = "my_pref";

    private Context context;
    private SharedPreferences preferences;

    private LiveData<String> live;
    private List<String> observed;
    private Observer<String> observer;

    @Before
    @UiThreadTest
    public void setup() {
        context = InstrumentationRegistry.getInstrumentation().getContext();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putString(PREFERENCE_KEY, "initial").apply();

        live = SharedPreferenceLiveData.forString(preferences, PREFERENCE_KEY, "default");
        observed = new ArrayList<>();
        observer = observed::add;
        live.observeForever(observer);
    }

    @After
    @UiThreadTest
    public void tearDown() {
        live.removeObserver(observer);
    }

    @Test
    public void test() {
        preferences.edit().putString(PREFERENCE_KEY, "foo").apply();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {}); // wait for listener to run on main thread

        preferences.edit().remove(PREFERENCE_KEY).apply();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {}); // wait for listener to run on main thread

        preferences.edit().putString(PREFERENCE_KEY, "bar").apply();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {}); // wait for listener to run on main thread

        assertEquals(List.of("initial", "foo", "default", "bar"), observed);
    }
}