package com.jonahbauer.qed.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
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
import com.jonahbauer.qed.Pref;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.eventSheet.EventBottomSheet;
import com.jonahbauer.qed.activities.mainFragments.ChatDatabaseFragment;
import com.jonahbauer.qed.activities.mainFragments.ChatFragment;
import com.jonahbauer.qed.activities.mainFragments.EventDatabaseFragment;
import com.jonahbauer.qed.activities.mainFragments.GalleryFragment;
import com.jonahbauer.qed.activities.mainFragments.LogFragment;
import com.jonahbauer.qed.activities.mainFragments.PersonDatabaseFragment;
import com.jonahbauer.qed.activities.mainFragments.QEDFragment;
import com.jonahbauer.qed.activities.personSheet.PersonBottomSheet;

import java.util.HashMap;
import java.util.Map;

import static com.jonahbauer.qed.DeepLinkingActivity.QEDIntent;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, NetworkListener, DrawerLayout.DrawerListener {
    private static final String FRAGMENT_TAG = "com.jonahbauer.qed.activities.QEDFragment";

    private SparseArray<QEDFragment> mCachedFragments = new SparseArray<>();

    private DrawerLayout mDrawerLayout;
    private NavigationView mNavView;
    private Toolbar mToolbar;
    private SharedPreferences mSharedPreferences;

    private boolean mDoubleBackToExitPressedOnce;

    private FragmentTransaction mFragmentTransaction;
    private int mNewSelection;

    private boolean mWaitForDropResult;

    private boolean mShouldReloadFragment;

    private QEDFragment mFragment = null;

    private Toolbar mAltToolbar;
    private boolean mAltToolbarBorrowed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        mNewSelection = mSharedPreferences.getInt(Pref.General.DRAWER_SELECTION, R.id.nav_chat);

        Intent intent = getIntent();
        if (!handleIntent(intent, this)) {
            super.finish();
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
            return;
        }


        if (!mSharedPreferences.getBoolean(Pref.General.LOGGED_IN,false)) {
            Intent intent2 = new Intent(this, LoginActivity.class);
            startActivity(intent2);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        mDrawerLayout = findViewById(R.id.drawer_layout);
        mNavView = findViewById(R.id.nav_view);
        mToolbar = findViewById(R.id.toolbar);
        mAltToolbar = findViewById(R.id.alt_toolbar);

        mNavView.setNavigationItemSelectedListener(this);
        mDrawerLayout.addDrawerListener(this);

        final RelativeLayout navHeader = (RelativeLayout) mNavView.getHeaderView(0);
        ViewTreeObserver vto = mNavView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mNavView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int width = mNavView.getMeasuredWidth();

                navHeader.getLayoutParams().height = width * 9 / 16;
                navHeader.requestLayout();
            }
        });


        setSupportActionBar(mToolbar);
        ActionBar actionbar = getSupportActionBar();
        assert actionbar != null;
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);

        mNavView.setCheckedItem(mSharedPreferences.getInt(Pref.General.DRAWER_SELECTION, R.id.nav_chat));
        reloadFragment(false);
    }

    @Override
    protected void onResume() {
        reloadFragment(true);
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (!mDoubleBackToExitPressedOnce) {
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
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        if (mWaitForDropResult) {
            if (BuildConfig.DEBUG) Log.d(Application.LOG_TAG_DEBUG, "onBackPressed() invoked while waiting for onDrop result");
            return;
        }

        // Close drawer if open
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawers();
            return;
        }

        // Hide visible alternative toolbar
        if (mAltToolbarBorrowed) {
            returnAltToolbar();
            return;
        }

        // Require double back to exit
        if (mDoubleBackToExitPressedOnce) {
            Boolean mayDrop = mFragment.onDrop(false);

            if (mayDrop == null) {
                mFragmentTransaction = null;
                mWaitForDropResult = true;
            } else if (mayDrop) {
                super.onBackPressed();
            }
        } else {
            this.mDoubleBackToExitPressedOnce = true;
            Toast.makeText(this, R.string.double_back_to_exit_toast, Toast.LENGTH_SHORT).show();
        }

        new Handler().postDelayed(() -> mDoubleBackToExitPressedOnce = false, 2000);
    }

    /**
     * Called by fragments that returned {@code null} in {@link QEDFragment#onDrop(boolean)}
     *
     * @param result if the fragment may be dropped
     * @see QEDFragment#onDrop(boolean)
     */
    public void onDropResult(boolean result) {
        if (mWaitForDropResult) {
            mWaitForDropResult = false;

            if (result) {
                if (mFragmentTransaction != null) {
                    commitFragmentTransaction();
                } else {
                    super.onBackPressed();
                }
            }
        } else {
            if (BuildConfig.DEBUG) Log.d(Application.LOG_TAG_DEBUG, "dropResult() invoked while not waiting for onDrop result.");
        }

        disposeFragmentTransaction();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            mDrawerLayout.openDrawer(GravityCompat.START);
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
            default: {
                if (mSharedPreferences.getInt(Pref.General.DRAWER_SELECTION, R.id.nav_chat) != menuItem.getItemId() && mNewSelection != menuItem.getItemId()) {
                    mNewSelection = menuItem.getItemId();
                    mShouldReloadFragment = true;
                }
                mDrawerLayout.closeDrawers();

                return false;
            }
        }
    }


    private void commitFragmentTransaction() {
        if (mFragmentTransaction != null) {
            mFragmentTransaction.commit();
            mFragmentTransaction = null;
        }

        mSharedPreferences.edit().putInt(Pref.General.DRAWER_SELECTION, mNewSelection).apply();
        mNavView.setCheckedItem(mNewSelection);
        mNewSelection = 0;
    }

    private void disposeFragmentTransaction() {
        mFragmentTransaction = null;
        mNewSelection = mSharedPreferences.getInt(Pref.General.DRAWER_SELECTION, R.id.nav_chat);
    }

    /**
     * Reloads the content mFragment if and only if the content type changed.
     *
     * @param onlyTitle if true only the title will be changed an no mFragment transaction is initiated
     */
    private void reloadFragment(boolean onlyTitle) {
        if (mWaitForDropResult) {
            if (BuildConfig.DEBUG) Log.d(Application.LOG_TAG_DEBUG, "reloadFragment() invoked while waiting for onDrop result");
            return;
        }

        FragmentManager manager = getSupportFragmentManager();

        Fragment oldFrag2 = manager.findFragmentByTag(FRAGMENT_TAG);
        if (!(oldFrag2 instanceof QEDFragment)) oldFrag2 = null;
        QEDFragment oldFrag = (QEDFragment) oldFrag2;

        mFragmentTransaction = null;
        int selected = onlyTitle ? mSharedPreferences.getInt(Pref.General.DRAWER_SELECTION, R.id.nav_chat) : mNewSelection;
        boolean changed = false;
        switch (selected) {
            case R.id.nav_chat:
                mToolbar.setTitle(getString(R.string.title_fragment_chat));
                if (!onlyTitle) {
                    mFragment = mCachedFragments.get(R.id.nav_chat);

                    if (mFragment == null) {
                        mFragment = ChatFragment.newInstance(R.style.AppTheme);
                        mCachedFragments.put(R.id.nav_chat, mFragment);
                    }

                    changed = true;
                }
                break;
            case R.id.nav_chat_db:
                mToolbar.setTitle(getString(R.string.title_fragment_chat_database));
                if (!onlyTitle) {
                    mFragment = mCachedFragments.get(R.id.nav_chat_db);

                    if (mFragment == null) {
                        mFragment = ChatDatabaseFragment.newInstance(R.style.AppTheme);
                        mCachedFragments.put(R.id.nav_chat_db, mFragment);
                    }

                    changed = true;
                }
                break;
            case R.id.nav_database_persons:
                mToolbar.setTitle(getString(R.string.title_fragment_persons_database));
                if (!onlyTitle) {
                    mFragment = mCachedFragments.get(R.id.nav_database_persons);

                    if (mFragment == null) {
                        mFragment = PersonDatabaseFragment.newInstance(R.style.AppTheme);
                        mCachedFragments.put(R.id.nav_database_persons, mFragment);
                    }

                    changed = true;
                }
                break;
            case R.id.nav_chat_log:
                mToolbar.setTitle(getString(R.string.title_fragment_log));
                if (!onlyTitle) {
                    mFragment = mCachedFragments.get(R.id.nav_chat_log);

                    if (mFragment == null) {
                        mFragment = LogFragment.newInstance(R.style.AppTheme);
                        mCachedFragments.put(R.id.nav_chat_log, mFragment);
                    }

                    changed = true;
                }
                break;
            case R.id.nav_database_events:
                mToolbar.setTitle(getString(R.string.title_fragment_events_database));
                if (!onlyTitle) {
                    mFragment = mCachedFragments.get(R.id.nav_database_events);

                    if (mFragment == null) {
                        mFragment = EventDatabaseFragment.newInstance(R.style.AppTheme);
                        mCachedFragments.put(R.id.nav_database_events, mFragment);
                    }

                    changed = true;
                }
                break;
            case R.id.nav_gallery:
                mToolbar.setTitle(getString(R.string.title_fragment_gallery));
                if (!onlyTitle) {
                    mFragment = mCachedFragments.get(R.id.nav_gallery);

                    if (mFragment == null) {
                        mFragment = GalleryFragment.newInstance(R.style.AppTheme);
                        mCachedFragments.put(R.id.nav_gallery, mFragment);
                    }

                    changed = true;
                }
                break;
        }

        if (!onlyTitle && changed) {
            mFragmentTransaction = manager.beginTransaction();
            mFragmentTransaction.replace(R.id.fragment, mFragment, FRAGMENT_TAG);

            Boolean mayDrop = oldFrag == null;
            if (!mayDrop)
                mayDrop = oldFrag.onDrop(false);

            if (mayDrop == null) {
                mWaitForDropResult = true;
            } else if (mayDrop && mFragment != null) {
                commitFragmentTransaction();
            }
        }
    }

    /**
     * Relays a network connection failure event to the content mFragment
     */
    @Override
    public void onConnectionFail() {
        if (mFragment != null && mFragment instanceof NetworkListener) ((NetworkListener) mFragment).onConnectionFail();
    }

    /**
     * Relays a network connection regain event to the content mFragment
     */
    @Override
    public void onConnectionRegain() {
        if (mFragment != null && mFragment instanceof NetworkListener) ((NetworkListener) mFragment).onConnectionRegain();
    }

    /**
     * If {@code activity} is {@code null} this method determines if the {@code intent} is of interest and returns {@code true} or {@code false} accordingly.
     *
     * If {@code activity} is non-null the appropriate actions (changing the content mFragment, showing a bottom sheet, ...) will be performed.
     *
     * @param intent an intent
     * @param activity the current main activity, may be null
     * @return true iff the intent is of interest to the {@code MainActivity}
     */
    public static boolean handleIntent(Intent intent, @Nullable MainActivity activity) {
        if (activity != null) activity.mShouldReloadFragment = false;

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
                        if (path.startsWith("/personen.php") || path.startsWith("/person.php")) {
                            String person = queries.getOrDefault("person", null);
                            if (activity != null) {
                                if (person != null && person.matches("\\d{1,5}")) {
                                    PersonBottomSheet personBottomSheet = PersonBottomSheet.newInstance(Long.parseLong(person));
                                    personBottomSheet.show(activity.getSupportFragmentManager(), personBottomSheet.getTag());
                                    activity.mShouldReloadFragment = false;
                                } else {
                                    activity.mNewSelection = R.id.nav_database_persons;
                                    activity.mShouldReloadFragment = true;
                                }
                            }

                            return true;
                        } else if (path.startsWith("/veranstaltungen.php") || path.startsWith("/veranstaltung.php")) {
                            String event = queries.getOrDefault("veranstaltung", null);
                            if (activity != null) {
                                if (event != null && event.matches("\\d{1,5}")) {
                                    try {
                                        long eventId = Long.parseLong(event);
                                        EventBottomSheet eventBottomSheet = EventBottomSheet.newInstance(eventId);
                                        eventBottomSheet.show(activity.getSupportFragmentManager(), eventBottomSheet.getTag());
                                        activity.mShouldReloadFragment = false;
                                    } catch (NumberFormatException ignored) {}
                                } else {
                                    activity.mNewSelection = R.id.nav_database_events;
                                    activity.mShouldReloadFragment = true;
                                }
                            }

                            return true;
                        }
                    }
                // QED-Chat
                } else if (host.equals("chat.qed-verein.de")) {
                    if (path != null) {
                        if (path.startsWith("/index.html")) {
                            if (activity != null) {
                                SharedPreferences.Editor editor = activity.mSharedPreferences.edit();
                                editor.putString(Pref.Chat.CHANNEL, queries.getOrDefault("channel", ""));
                                editor.apply();

                                activity.mNewSelection = R.id.nav_chat;

                                activity.mShouldReloadFragment = true;
                            }

                            return true;
                        } else if (path.startsWith("/rubychat/history")) {
                            if (activity != null) {
                                SharedPreferences.Editor editor = activity.mSharedPreferences.edit();
                                editor.putString(Pref.Chat.CHANNEL, queries.getOrDefault("channel", ""));
                                editor.apply();

                                activity.mNewSelection = R.id.nav_chat_log;

                                activity.mShouldReloadFragment = true;
                            }

                            return true;
                        }
                    }
                // QED-Gallery
                } else if (host.equals("qedgallery.qed-verein.de")) {
                    if (path != null) {
                        if (path.startsWith("/album_list.php")) {
                            if (activity != null) {
                                activity.mNewSelection = R.id.nav_gallery;
                                activity.mShouldReloadFragment = true;
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
            if (mShouldReloadFragment) {
                mShouldReloadFragment = false;
                new Handler(Looper.getMainLooper()).post(() -> reloadFragment(false));
            }
        } else {
            super.finish();
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
            return;
        }

        super.onNewIntent(intent);
    }

    /**
     * Borrows the alternative toolbar {@link #mAltToolbar} which will be shown atop the standard one.
     *
     * The toolbar will be cleaned (menu and listeners removed) every time this method is called.
     *
     * After the alternative toolbar is no longer needed {@link #returnAltToolbar()} must be called in
     * order to hide the toolbar and make it accessible for other fragments.
     *
     * @return the alternative toolbar
     * @throws IllegalStateException when the alternative toolbar is has already been borrowed and is not yet returned
     *
     * @see #returnAltToolbar()
     */
    public Toolbar borrowAltToolbar() {
        if (mAltToolbarBorrowed) throw new IllegalStateException("borrowAltToolbar may only be called once before calling returnAltToolbar!");

        mAltToolbarBorrowed = true;

        mAltToolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        mAltToolbar.setOnMenuItemClickListener(null);
        mAltToolbar.setNavigationOnClickListener(null);
        mAltToolbar.setVisibility(View.VISIBLE);
        mAltToolbar.getMenu().clear();

        TypedValue colorPrimary = new TypedValue();
        getTheme().resolveAttribute(R.attr.colorPrimary, colorPrimary, true);

        getWindow().setStatusBarColor(colorPrimary.data);

        return mAltToolbar;
    }

    /**
     * @see #borrowAltToolbar()
     */
    public void returnAltToolbar() {
        if (mAltToolbarBorrowed) {
            if (mFragment != null) mFragment.revokeAltToolbar();

            mAltToolbar.setVisibility(View.GONE);

            TypedValue colorPrimaryDark = new TypedValue();
            getTheme().resolveAttribute(R.attr.colorPrimaryDark, colorPrimaryDark, true);

            getWindow().setStatusBarColor(colorPrimaryDark.data);

            mAltToolbarBorrowed = false;
        }
    }

    @Override
    public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {}

    @Override
    public void onDrawerOpened(@NonNull View drawerView) {}

    @Override
    public void onDrawerClosed(@NonNull View drawerView) {
        if (mShouldReloadFragment) {
            reloadFragment(false);
            mShouldReloadFragment = false;
        }
    }

    @Override
    public void onDrawerStateChanged(int newState) {}
}
