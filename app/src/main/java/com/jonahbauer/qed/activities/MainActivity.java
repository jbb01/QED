package com.jonahbauer.qed.activities;

import android.content.Intent;
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

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.navigation.NavigationView;
import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.BuildConfig;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.mainFragments.ChatDatabaseFragment;
import com.jonahbauer.qed.activities.mainFragments.ChatFragment;
import com.jonahbauer.qed.activities.mainFragments.EventDatabaseFragment;
import com.jonahbauer.qed.activities.mainFragments.GalleryFragment;
import com.jonahbauer.qed.activities.mainFragments.LogFragment;
import com.jonahbauer.qed.activities.mainFragments.PersonDatabaseFragment;
import com.jonahbauer.qed.activities.mainFragments.QEDFragment;
import com.jonahbauer.qed.activities.settings.RootSettingsActivity;
import com.jonahbauer.qed.networking.NetworkListener;
import com.jonahbauer.qed.networking.login.QEDLogout;
import com.jonahbauer.qed.util.Preferences;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

import static com.jonahbauer.qed.DeepLinkingActivity.QEDIntent;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, NetworkListener, DrawerLayout.DrawerListener {
    private static final String FRAGMENT_TAG = "com.jonahbauer.qed.activities.QEDFragment";

    private final SparseArray<QEDFragment> mCachedFragments = new SparseArray<>();

    private DrawerLayout mDrawerLayout;
    private NavigationView mNavView;
    private Toolbar mToolbar;

    private boolean mDoubleBackToExitPressedOnce;

    // when changing fragments the onDrop method of the current fragment is called.
    // if the result is null, i.e. the output could not be determined immediately, e.g. because it
    // has to wait for user input, then the mWaitForDropResult flag will be set.
    private boolean mWaitForDropResult;
    // if the cause for mWaitForDropResult being set is a change of fragments the mFragmentTransaction
    // will be a transaction that realizes that change of fragments.
    // if the cause for mWaitForDropResult being set is a back press then mFragmentTransaction will be null
    private FragmentTransaction mFragmentTransaction;
    // the new drawer selection that will become active after a call to reloadFragment(false)
    private DrawerSelection mNewSelection;

    // used by onNavigationItemSelected and handleIntent in order to realize delayed change of fragment
    private boolean mShouldReloadFragment;

    private QEDFragment mFragment = null;

    private Toolbar mAltToolbar;
    private boolean mAltToolbarBorrowed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_App_NoActionBar);
        super.onCreate(savedInstanceState);

        mNewSelection = Preferences.general().getDrawerSelection();

        Intent intent = getIntent();
        if (!handleIntent(intent, this)) {
            super.finish();
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
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

            QEDFragment oldFragment = asQedFragment(manager.findFragmentByTag(FRAGMENT_TAG));

            if (oldFragment != null)
                oldFragment.onDrop(true);
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
            mDoubleBackToExitPressedOnce = false;

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
            new Handler().postDelayed(() -> mDoubleBackToExitPressedOnce = false, 2000);
        }
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

        // relay selection to fragment
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment);
        if (fragment != null) return fragment.onOptionsItemSelected(item);
        else return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.nav_settings) {
            Intent intent = new Intent(this, RootSettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.nav_logout) {
            QEDLogout.logoutAsync().thenAccept(success -> {
                if (!success) {
                    Log.e(Application.LOG_TAG_ERROR, "An error occured during logout.");
                }
            });

            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return true;
        } else {
            DrawerSelection selection = DrawerSelection.byMenuItem(menuItem, null);
            if (selection != null && selection != Preferences.general().getDrawerSelection() && selection != mNewSelection) {
                // fragment will not be changed immediately but only after the drawer is closed
                mNewSelection = selection;
                mShouldReloadFragment = true;
                mDrawerLayout.closeDrawers();
                return true;
            }
        }

        return false;
    }

    /**
     * Commit the stored {@link #mFragmentTransaction} and sets the persistent drawer selection
     * according to {@link #mNewSelection}.
     */
    private void commitFragmentTransaction() {
        if (mFragmentTransaction != null) {
            mFragmentTransaction.commit();
            mFragmentTransaction = null;
        }

        Preferences.general().edit().setDrawerSelection(mNewSelection).apply();
        mNavView.setCheckedItem(mNewSelection.getMenuItemId());
        mNewSelection = null;
    }

    /**
     * Disposes of the stored {@link #mFragmentTransaction} and {@link #mNewSelection}.
     */
    private void disposeFragmentTransaction() {
        mFragmentTransaction = null;
        mNewSelection = null;
    }

    /**
     * Reloads the fragment.
     * <br>
     * If {@code titleOnly} is set, then only the title will be changed according to the
     * {@linkplain Preferences.General#getDrawerSelection() persistent selection}.
     * <br>
     * Otherwise if {@link #mNewSelection} is set or the current fragment does not match the
     * {@linkplain Preferences.General#getDrawerSelection() persistent selection} a fragment transaction is initiated
     * in order to change to the new selection. The {@link QEDFragment#onDrop(boolean)} method is
     * called before committing the transaction.
     *
     * @param titleOnly if true only the title will be changed an no {@code FragmentTransaction} is initiated
     */
    private void reloadFragment(boolean titleOnly) {
        if (mWaitForDropResult) {
            if (BuildConfig.DEBUG) Log.d(Application.LOG_TAG_DEBUG, "reloadFragment() invoked while waiting for onDrop result");
            return;
        }
        mFragmentTransaction = null;

        DrawerSelection selected = (titleOnly || mNewSelection == null)
                ? Preferences.general().getDrawerSelection()
                : mNewSelection;

        mToolbar.setTitle(getString(selected.getTitle()));

        if (!titleOnly) {
            FragmentManager manager = getSupportFragmentManager();

            QEDFragment oldQedFragment = asQedFragment(manager.findFragmentByTag(FRAGMENT_TAG));

            if (oldQedFragment == null || selected.getFragmentClass() != oldQedFragment.getClass()) {
                mFragment = mCachedFragments.get(selected.getId());
                if (mFragment == null) {
                    // cache miss
                    switch (selected) {
                        case CHAT:
                            mFragment = ChatFragment.newInstance(R.style.Theme_App);
                            break;
                        case CHAT_DATABASE:
                            mFragment = ChatDatabaseFragment.newInstance(R.style.Theme_App);
                            break;
                        case CHAT_LOG:
                            mFragment = LogFragment.newInstance(R.style.Theme_App);
                            break;
                        case DATABASE_PEOPLE:
                            mFragment = PersonDatabaseFragment.newInstance(R.style.Theme_App);
                            break;
                        case DATABASE_EVENTS:
                            mFragment = EventDatabaseFragment.newInstance(R.style.Theme_App);
                            break;
                        case GALLERY:
                            mFragment = GalleryFragment.newInstance(R.style.Theme_App);
                            break;
                        default:
                            throw new AssertionError();
                    }
                    mCachedFragments.put(selected.getId(), mFragment);
                }

                assert mFragment != null;

                mFragmentTransaction = manager.beginTransaction();
                mFragmentTransaction.replace(R.id.fragment, mFragment, FRAGMENT_TAG);

                Boolean mayDrop = oldQedFragment == null;
                if (!mayDrop)
                    mayDrop = oldQedFragment.onDrop(false);

                if (mayDrop == null) {
                    mWaitForDropResult = true;
                } else if (mayDrop) {
                    commitFragmentTransaction();
                }
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

        if (intent.getAction() == null) {
            if (activity != null) activity.mShouldReloadFragment = true;
            return true;
        }

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
                // TODO anpassen an neue datenbank
                if (host != null) if (host.equals("qeddb.qed-verein.de")) {
                    if (path != null) {
                        if (path.startsWith("/personen.php") || path.startsWith("/person.php")) {
                            String person = queries.getOrDefault("person", null);
                            if (activity != null) {
                                if (person != null && person.matches("\\d{1,5}")) {
                                    long personId = Long.parseLong(person);
                                    // TODO show person bottom sheet
                                    activity.mShouldReloadFragment = false;
                                } else {
                                    activity.mNewSelection = DrawerSelection.DATABASE_PEOPLE;
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
                                        // TODO show event bottom sheet
                                        activity.mShouldReloadFragment = false;
                                    } catch (NumberFormatException ignored) {}
                                } else {
                                    activity.mNewSelection = DrawerSelection.DATABASE_EVENTS;
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
                                Preferences.chat().edit().setChannel(queries.getOrDefault("channel", "")).apply();

                                activity.mNewSelection = DrawerSelection.CHAT;
                                activity.mShouldReloadFragment = true;
                            }

                            return true;
                        } else if (path.startsWith("/rubychat/history")) {
                            if (activity != null) {
                                Preferences.chat().edit().setChannel(queries.getOrDefault("channel", "")).apply();

                                activity.mNewSelection = DrawerSelection.CHAT_LOG;
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
                                activity.mNewSelection = DrawerSelection.GALLERY;
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

    @Getter
    public enum DrawerSelection {
        CHAT(1, R.string.title_fragment_chat, R.id.nav_chat, ChatFragment.class),
        CHAT_LOG(2, R.string.title_fragment_log, R.id.nav_chat_log, LogFragment.class),
        CHAT_DATABASE(3, R.string.title_fragment_chat_database, R.id.nav_chat_db, ChatDatabaseFragment.class),
        DATABASE_PEOPLE(4, R.string.title_fragment_persons_database, R.id.nav_database_persons, PersonDatabaseFragment.class),
        DATABASE_EVENTS(5, R.string.title_fragment_events_database, R.id.nav_database_events, EventDatabaseFragment.class),
        GALLERY(6, R.string.title_fragment_gallery, R.id.nav_gallery, GalleryFragment.class);

        private final int id;
        @StringRes
        private final int title;
        @IdRes
        private final int menuItemId;

        private final Class<? extends QEDFragment> fragmentClass;

        DrawerSelection(int id, @StringRes int title, int menuItemId, Class<? extends QEDFragment> fragmentClass) {
            this.id = id;
            this.title = title;
            this.menuItemId = menuItemId;
            this.fragmentClass = fragmentClass;
        }

        public static DrawerSelection byId(int id, DrawerSelection defaultValue) {
            switch (id) {
                case 1: return CHAT;
                case 2: return CHAT_LOG;
                case 3: return CHAT_DATABASE;
                case 4: return DATABASE_PEOPLE;
                case 5: return DATABASE_EVENTS;
                case 6: return GALLERY;
                default: return defaultValue;
            }
        }

        public static DrawerSelection byMenuItem(MenuItem menuItem, DrawerSelection defaultValue) {
            for (DrawerSelection value : values()) {
                if (menuItem.getItemId() == value.getMenuItemId()) {
                    return value;
                }
            }
            return defaultValue;
        }
    }

    private static QEDFragment asQedFragment(Fragment fragment) {
        if (fragment instanceof QEDFragment) return (QEDFragment) fragment;
        else return null;
    }
}
