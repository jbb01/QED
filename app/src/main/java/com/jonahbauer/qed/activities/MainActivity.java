package com.jonahbauer.qed.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import com.google.android.material.navigation.NavigationView;
import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.BuildConfig;
import com.jonahbauer.qed.NetworkListener;
import com.jonahbauer.qed.R;

import java.util.HashMap;
import java.util.Map;

import static com.jonahbauer.qed.DeepLinkingActivity.QEDIntent;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, NetworkListener {
    private static final String FRAGMENT_TAG = "com.jonahbauer.qed.activities.QEDFragment";

    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private Toolbar toolbar;
    private SharedPreferences sharedPreferences;

    private boolean doubleBackToExitPressedOnce;

    private FragmentTransaction fragmentTransaction;
    private boolean waitForDropResult;

    private boolean shouldReloadFragment;

    private QEDFragment fragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        Intent intent = getIntent();
        if (!handleIntent(intent, this)) {
            super.finish();
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
            return;
        }


        if (!sharedPreferences.getBoolean(getString(R.string.preferences_loggedIn_key),false)) {
            Intent intent2 = new Intent(this, LoginActivity.class);
            startActivity(intent2);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);

        navView.setNavigationItemSelectedListener(this);
        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View view, float v) {}

            @Override
            public void onDrawerOpened(@NonNull View view) {}

            @Override
            public void onDrawerClosed(@NonNull View view) {
                if (shouldReloadFragment) {
                    reloadFragment(false);
                    shouldReloadFragment = false;
                }
            }

            @Override
            public void onDrawerStateChanged(int i) {}
        });


        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        assert actionbar != null;
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);

        navView.setCheckedItem(sharedPreferences.getInt(getString(R.string.preferences_drawerSelection_key), R.id.nav_chat));
        reloadFragment(false);
    }

    @Override
    protected void onResume() {
        reloadFragment(true);
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (!doubleBackToExitPressedOnce) {
            FragmentManager manager = getSupportFragmentManager();

            Fragment oldFrag2 = manager.findFragmentByTag(FRAGMENT_TAG);
            if (!(oldFrag2 instanceof QEDFragment)) oldFrag2 = null;
            QEDFragment oldFrag = (QEDFragment) oldFrag2;

            if (oldFrag != null)
                oldFrag.onDrop(true);
        }

        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (waitForDropResult) {
            if (BuildConfig.DEBUG) Log.d(Application.LOG_TAG_DEBUG, "onBackPressed() invoked while waiting for onDrop result");
            return;
        }

        // Close drawer if open
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawers();
            return;
        }

        // Require double back to exit
        if (doubleBackToExitPressedOnce) {
            Boolean mayDrop = fragment.onDrop(false);

            if (mayDrop == null) {
                fragmentTransaction = null;
                waitForDropResult = true;
            } else if (mayDrop) {
                super.onBackPressed();
            }
        } else {
            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(this, R.string.double_back_to_exit_toast, Toast.LENGTH_SHORT).show();
        }

        new Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
    }

    public void onDropResult(boolean result) {
        if (waitForDropResult) {
            waitForDropResult = false;

            if (result) {
                if (fragmentTransaction != null) fragmentTransaction.commit();
                else super.onBackPressed();
            }
        } else {
            if (BuildConfig.DEBUG) Log.d(Application.LOG_TAG_DEBUG, "dropResult() invoked while not waiting for onDrop result.");
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            drawerLayout.openDrawer(GravityCompat.START);
            return true;
        }

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment);
        if (fragment != null) return fragment.onOptionsItemSelected(item);
        else return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.nav_settings: {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true; }
            case R.id.nav_logout: {
                // Overwrite all stored credentials
                Application application = (Application) getApplication();
                application.saveData(Application.KEY_USERID, "", false);
                application.saveData(Application.KEY_USERNAME, "", false);
                application.saveData(Application.KEY_PASSWORD, "", true);
                application.saveData(Application.KEY_CHAT_PWHASH, "", true);
                application.saveData(Application.KEY_DATABASE_SESSIONID, "", true);
                application.saveData(Application.KEY_DATABASE_SESSIONID2, "", true);
                application.saveData(Application.KEY_DATABASE_SESSION_COOKIE, "", true);
                application.saveData(Application.KEY_GALLERY_PHPSESSID, "", true);
                application.saveData(Application.KEY_GALLERY_PWHASH, "", true);
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                finish();
                return true; }
        }

        if (sharedPreferences.getInt(getString(R.string.preferences_drawerSelection_key),R.id.nav_chat) != menuItem.getItemId()) {
            sharedPreferences.edit()
                    .putInt(getString(R.string.preferences_drawerSelection_key), menuItem.getItemId()).apply();
            shouldReloadFragment = true;
//            navView.postDelayed(() -> reloadFragment(false), 250);
        }
        drawerLayout.closeDrawers();

        return true;
    }

    /**
     * Reloads the content fragment if and only if the content type changed.
     *
     * @param onlyTitle if true only the title will be changed an no fragment transaction is initiated
     */
    private void reloadFragment(boolean onlyTitle) {
        if (waitForDropResult) {
            if (BuildConfig.DEBUG) Log.d(Application.LOG_TAG_DEBUG, "reloadFragment() invoked while waiting for onDrop result");
            return;
        }

        FragmentManager manager = getSupportFragmentManager();

        Fragment oldFrag2 = manager.findFragmentByTag(FRAGMENT_TAG);
        if (!(oldFrag2 instanceof QEDFragment)) oldFrag2 = null;
        QEDFragment oldFrag = (QEDFragment) oldFrag2;

        fragmentTransaction = null;
        int selected = sharedPreferences.getInt(getString(R.string.preferences_drawerSelection_key), R.id.nav_chat);
        switch (selected) {
            case R.id.nav_chat:
                toolbar.setTitle(getString(R.string.title_fragment_chat));
                if (oldFrag instanceof ChatFragment) {
                    fragment = oldFrag;
                    return;
                }

                if (!onlyTitle) {
                    fragment = new ChatFragment();
                    fragmentTransaction = manager.beginTransaction();
                    fragmentTransaction.replace(R.id.fragment, fragment, FRAGMENT_TAG);
                }
                break;
            case R.id.nav_chat_db:
                toolbar.setTitle(getString(R.string.title_fragment_chat_database));
                if (oldFrag instanceof ChatDatabaseFragment) {
                    fragment = oldFrag;
                    return;
                }

                if (!onlyTitle) {
                    fragment = new ChatDatabaseFragment();
                    fragmentTransaction = manager.beginTransaction();
                    fragmentTransaction.replace(R.id.fragment, fragment, FRAGMENT_TAG);
                }
                break;
            case R.id.nav_database_persons:
                toolbar.setTitle(getString(R.string.title_fragment_persons_database));
                if (oldFrag instanceof PersonDatabaseFragment) {
                    fragment = oldFrag;
                    return;
                }

                if (!onlyTitle) {
                    fragment = new PersonDatabaseFragment();
                    fragmentTransaction = manager.beginTransaction();
                    fragmentTransaction.replace(R.id.fragment, fragment, FRAGMENT_TAG);
                }
                break;
            case R.id.nav_chat_log:
                toolbar.setTitle(getString(R.string.title_fragment_log));
                if (oldFrag instanceof LogFragment) {
                    fragment = oldFrag;
                    return;
                }

                if (!onlyTitle) {
                    fragment = new LogFragment(LogFragment.Mode.DATE_RECENT, "", 86400L);
                    fragmentTransaction = manager.beginTransaction();
                    fragmentTransaction.replace(R.id.fragment, fragment, FRAGMENT_TAG);
                }
                break;
            case R.id.nav_database_events:
                toolbar.setTitle(getString(R.string.title_fragment_events_database));
                if (oldFrag instanceof EventDatabaseFragment) {
                    fragment = oldFrag;
                    return;
                }

                if (!onlyTitle) {
                    fragment = new EventDatabaseFragment();
                    fragmentTransaction = manager.beginTransaction();
                    fragmentTransaction.replace(R.id.fragment, fragment, FRAGMENT_TAG);
                }
                break;
            case R.id.nav_gallery:
                toolbar.setTitle(getString(R.string.title_fragment_gallery));
                if (oldFrag instanceof GalleryFragment) {
                    fragment = oldFrag;
                    return;
                }

                if (!onlyTitle) {
                    fragment = new GalleryFragment();
                    fragmentTransaction = manager.beginTransaction();
                    fragmentTransaction.replace(R.id.fragment, fragment, FRAGMENT_TAG);
                }
                break;
        }

        navView.setCheckedItem(selected);

        Boolean mayDrop = oldFrag == null;
        if (!mayDrop)
            mayDrop = oldFrag.onDrop(false);

        if (mayDrop == null) {
            waitForDropResult = true;
        } else {
            if (!onlyTitle && fragmentTransaction != null && mayDrop) fragmentTransaction.commit();
        }
    }

    /**
     * Relays a network connection failure event to the content fragment
     */
    @Override
    public void onConnectionFail() {
        if (fragment != null && fragment instanceof NetworkListener) ((NetworkListener) fragment).onConnectionFail();
    }

    /**
     * Relays a network connection regain event to the content fragment
     */
    @Override
    public void onConnectionRegain() {
        if (fragment != null && fragment instanceof NetworkListener) ((NetworkListener) fragment).onConnectionRegain();
    }

    /**
     * If {@param activity} is null this method determines if the {@param intent} is of interest and {@return} true or false accordingly.
     *
     * If {@param activity} is non-null the appropriate actions (changing the content fragment, showing a bottom sheet, ...) will be performed.
     */
    public static boolean handleIntent(Intent intent, @Nullable MainActivity activity) {
        if (activity != null) activity.shouldReloadFragment = false;

        if (Intent.ACTION_MAIN.equals(intent.getAction()) && intent.getCategories().contains(Intent.CATEGORY_LAUNCHER)) return true;

        if (Intent.ACTION_VIEW.equals(intent.getAction()) || QEDIntent.ACTION_SHOW.equals(intent.getAction())) {
            Uri data = intent.getData();
            if (data != null) {
                String host = data.getHost();
                String path = data.getPath();

                String query = data.getQuery();
                Map<String,String> queries = new HashMap<>();
                if (query != null) for (String q : query.split("&")) {
                    String[] parts = q.split("=");
                    if (parts.length > 1) queries.put(parts[0], parts[1]);
                    else if (parts.length > 0) queries.put(parts[0], "");
                }

                // QED-DB
                if (host != null) if (host.equals("qeddb.qed-verein.de")) {
                    if (path != null) {
                        if (path.startsWith("/personen.php")) {
                            String person = queries.getOrDefault("person", null);
                            if (activity != null) {
                                if (person != null && person.matches("\\d+")) {
                                    PersonBottomSheet personBottomSheet = PersonBottomSheet.newInstance(person);
                                    personBottomSheet.show(activity.getSupportFragmentManager(), personBottomSheet.getTag());
                                    activity.shouldReloadFragment = false;
                                } else {
                                    activity.sharedPreferences.edit().putInt(activity.getString(R.string.preferences_drawerSelection_key), R.id.nav_database_persons).apply();
                                    activity.shouldReloadFragment = true;
                                }
                            }

                            return true;
                        } else if (path.startsWith("/veranstaltungen.php")) {
                            if (activity != null) {
                                activity.sharedPreferences.edit().putInt(activity.getString(R.string.preferences_drawerSelection_key), R.id.nav_database_events).apply();
                                activity.shouldReloadFragment = true;
                            }

                            String event = queries.getOrDefault("veranstaltung", null);
                            if (event != null && event.matches("\\d+")) {
                                EventDatabaseFragment.showEventId = Integer.valueOf(event);
                                EventDatabaseFragment.shownEvent = false;
                            }

                            return true;
                        }
                    }
                // QED-Chat
                } else if (host.equals("chat.qed-verein.de")) {
                    if (path != null) {
                        if (path.startsWith("/index.html")) {
                            if (activity != null) {
                                SharedPreferences.Editor editor = activity.sharedPreferences.edit();
                                editor.putInt(activity.getString(R.string.preferences_drawerSelection_key), R.id.nav_chat);
                                editor.putString(activity.getString(R.string.preferences_chat_channel_key), queries.getOrDefault("channel", ""));
                                editor.apply();

                                activity.shouldReloadFragment = true;
                            }

                            return true;
                        } else if (path.startsWith("/rubychat/history")) {
                            if (activity != null) {
                                SharedPreferences.Editor editor = activity.sharedPreferences.edit();
                                editor.putInt(activity.getString(R.string.preferences_drawerSelection_key), R.id.nav_chat_log);
                                editor.putString(activity.getString(R.string.preferences_chat_channel_key), queries.getOrDefault("channel", ""));
                                editor.apply();

                                activity.shouldReloadFragment = true;
                            }

                            return true;
                        }
                    }
                // QED-Gallery
                } else if (host.equals("qedgallery.qed-verein.de")) {
                    if (path != null) {
                        if (path.startsWith("/album_list.php")) {
                            if (activity != null) {
                                SharedPreferences.Editor editor = activity.sharedPreferences.edit();
                                editor.putInt(activity.getString(R.string.preferences_drawerSelection_key), R.id.nav_gallery).apply();
                                activity.shouldReloadFragment = true;
                            }

                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (handleIntent(intent,this)) {
            if (shouldReloadFragment) {
                shouldReloadFragment = false;
                reloadFragment(false);
            }
        } else {
            super.finish();
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
            return;
        }

        super.onNewIntent(intent);
    }
}
