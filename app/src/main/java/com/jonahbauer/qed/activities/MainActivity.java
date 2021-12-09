package com.jonahbauer.qed.activities;

import static com.jonahbauer.qed.activities.DeepLinkingActivity.QEDIntent;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.util.Pair;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.navigation.NavigationView;
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
import com.jonahbauer.qed.databinding.ActivityMainBinding;
import com.jonahbauer.qed.model.LogRequest;
import com.jonahbauer.qed.networking.NetworkListener;
import com.jonahbauer.qed.networking.login.QEDLogout;
import com.jonahbauer.qed.util.Preferences;

import java.util.function.Function;

import lombok.Getter;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, NetworkListener, DrawerLayout.DrawerListener {
    private static final String LOG_TAG = MainActivity.class.getName();
    private static final String FRAGMENT_TAG = "com.jonahbauer.qed.activities.QEDFragment";

    private ActivityMainBinding mBinding;

    private boolean mDoubleBackToExitPressedOnce;

    /**
     * The next selection that will become active after a call to {@link #reloadFragment(boolean)}.
     */
    private Pair<DrawerSelection, Bundle> mNewFragment;
    private Pair<FragmentTransaction, QEDFragment> mFragmentTransaction;

    /**
     * The currently shown {@link QEDFragment}.
     */
    private QEDFragment mFragment = null;

    private ActionMode mActionMode;
    private boolean mWaitForDropResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_App_NoActionBar);
        super.onCreate(savedInstanceState);

        mNewFragment = Pair.create(
                Preferences.general().getDrawerSelection(),
                null
        );

        Intent intent = getIntent();
        if (!handleIntent(intent, this)) {
            super.finish();
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
            return;
        }

        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mBinding.navView.setNavigationItemSelectedListener(this);
        mBinding.drawerLayout.addDrawerListener(this);

        // setup navigation drawer header to be 16:9
        final RelativeLayout navHeader = (RelativeLayout) mBinding.navView.getHeaderView(0);
        ViewTreeObserver vto = mBinding.navView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mBinding.navView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int width = mBinding.navView.getMeasuredWidth();

                navHeader.getLayoutParams().height = width * 9 / 16;
                navHeader.requestLayout();
            }
        });

        // setup toolbar
        setSupportActionBar(mBinding.toolbar);
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
            if (mFragment != null) {
                mFragment.onDrop(true);
            }
        }

        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (mWaitForDropResult) {
            if (BuildConfig.DEBUG) Log.d(LOG_TAG, "onBackPressed() invoked while waiting for onDrop result");
            return;
        }

        // Close drawer if open
        if (mBinding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            mBinding.drawerLayout.closeDrawers();
            return;
        }


        // Require double back to exit
        if (mDoubleBackToExitPressedOnce) {
            mDoubleBackToExitPressedOnce = false;

            if (mFragment != null) {
                // allow fragment to intercept
                Boolean mayDrop = mFragment.onDrop(false);

                if (mayDrop == null) {
                    disposeFragmentTransaction();
                    mWaitForDropResult = true;
                } else if (mayDrop) {
                    super.onBackPressed();
                }
            } else {
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
            if (BuildConfig.DEBUG) Log.d(LOG_TAG, "dropResult() invoked while not waiting for onDrop result.");
        }

        disposeFragmentTransaction();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // open navigation drawer
        if (item.getItemId() == android.R.id.home) {
            mBinding.drawerLayout.openDrawer(GravityCompat.START);
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mBinding.getRoot().getWindowToken(), 0);
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
            QEDLogout.logoutAsync(success -> {
                if (!success) {
                    Log.e(LOG_TAG, "An error occured during logout.");
                }
            });

            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return true;
        } else {
            DrawerSelection selection = DrawerSelection.byMenuItem(menuItem, null);
            if (selection != null && selection != Preferences.general().getDrawerSelection()) {
                // fragment will not be changed immediately but only after the drawer is closed
                mNewFragment = Pair.create(selection, null);
                mBinding.drawerLayout.closeDrawers();
                return true;
            }
        }

        return false;
    }

    /**
     * Reloads the fragment.
     * <br>
     * If {@code titleOnly} is set, then only the title will be changed according to the
     * {@linkplain Preferences.General#getDrawerSelection() persistent selection}.
     * <br>
     * Otherwise if {@link #mNewFragment} is set or the current fragment does not match the
     * persistent selection a fragment transaction is initiated in order to change to the new
     * selection. The {@link QEDFragment#onDrop(boolean)} method is called before committing the
     * transaction.
     *
     * @param titleOnly if true only the title will be changed an no {@code FragmentTransaction} is initiated
     */
    private void reloadFragment(boolean titleOnly) {
        if (mWaitForDropResult) {
            if (BuildConfig.DEBUG) Log.d(LOG_TAG, "reloadFragment() invoked while waiting for onDrop result");
            return;
        }
        mFragmentTransaction = null;

        DrawerSelection persistentSelection = Preferences.general().getDrawerSelection();
        if (titleOnly) {
            mBinding.toolbar.setTitle(persistentSelection.getTitle());
        } else {
            if (mNewFragment == null) {
                if (mFragment == null || mFragment.getClass() != persistentSelection.getFragmentClass()) {
                    mNewFragment = Pair.create(persistentSelection, null);
                } else {
                    mBinding.toolbar.setTitle(persistentSelection.getTitle());
                }
            }

            if (mNewFragment != null) {
                // reload fragment
                QEDFragment newFragment = mNewFragment.first.getNewInstance().apply(R.style.Theme_App);
                if (newFragment.getArguments() == null) newFragment.setArguments(new Bundle());
                if (mNewFragment.second != null) newFragment.getArguments().putAll(mNewFragment.second);

                FragmentManager manager = getSupportFragmentManager();
                FragmentTransaction transaction = manager.beginTransaction();
                transaction.replace(R.id.fragment, newFragment, FRAGMENT_TAG);

                mFragmentTransaction = Pair.create(transaction, newFragment);

                if (mFragment == null) {
                    commitFragmentTransaction();
                } else {
                    Boolean mayDrop = mFragment.onDrop(false);
                    if (mayDrop == null) {
                        mWaitForDropResult = true;
                    } else if (mayDrop) {
                        commitFragmentTransaction();
                    } else {
                        disposeFragmentTransaction();
                    }
                }
            }
        }
    }

    /**
     * Commit the stored {@link #mFragmentTransaction} and sets the persistent drawer selection
     * according to {@link #mNewFragment}.
     */
    private void commitFragmentTransaction() {
        if (mNewFragment != null) {
            Preferences.general().edit().setDrawerSelection(mNewFragment.first).apply();
            mBinding.navView.setCheckedItem(mNewFragment.first.getMenuItemId());
            mBinding.toolbar.setTitle(getString(mNewFragment.first.getTitle()));

            if (mFragmentTransaction != null) {
                mFragmentTransaction.first.commit();
                mFragment = mFragmentTransaction.second;
            }
        }

        mNewFragment = null;
        mFragmentTransaction = null;
    }

    /**
     * Disposes of the stored {@link #mFragmentTransaction} and {@link #mNewFragment}.
     */
    private void disposeFragmentTransaction() {
        mNewFragment = null;
        mFragmentTransaction = null;

        // since onNavigationItemSelected doesn't know if transaction will be executed
        // the new selection is speculatively accepted and needs to be reversed in case of failure
        DrawerSelection selection = DrawerSelection.byClass(mFragment.getClass(), DrawerSelection.CHAT);
        mBinding.toolbar.setTitle(selection.getTitle());
        mBinding.navView.setCheckedItem(selection.getMenuItemId());
    }

    //<editor-fold desc="Network Listener" defaultstate="collapsed">
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
    //</editor-fold>

    //<editor-fold desc="Intent Handling" defaultstate="collapsed">
    /**
     * If {@code activity} is {@code null} this method determines if the {@code intent} is of interest and returns {@code true} or {@code false} accordingly.
     *
     * If {@code activity} is non-null the appropriate actions (changing the content mFragment, showing a bottom sheet, ...) will be performed.
     *
     * @param intent an intent
     * @param activity the current main activity, may be null
     * @return true iff the intent is of interest to the {@code MainActivity}
     */
    public static boolean handleIntent(@NonNull Intent intent, @Nullable MainActivity activity) {
        // launcher intent
        if (Intent.ACTION_MAIN.equals(intent.getAction()) && intent.getCategories().contains(Intent.CATEGORY_LAUNCHER)) return true;

        // explicit intent
        if (intent.getAction() == null) return true;

        // external intent via deep link
        if (Intent.ACTION_VIEW.equals(intent.getAction()) || QEDIntent.ACTION_SHOW.equals(intent.getAction())) {
            Uri data = intent.getData();

            if (data != null) {
                String host = data.getHost();
                if (host == null) return false;

                String path = data.getPath();

                switch (host) {
                    // DB
                    case "qeddb.qed-verein.de":
                        if (path == null) return false;

                        // people
                        if (path.startsWith("/people")) {
                            if (activity != null) {
                                activity.mNewFragment = Pair.create(DrawerSelection.DATABASE_PEOPLE, null);
                            }
                            return true;
                        }

                        // events
                        if (path.startsWith("/events")) {
                            if (activity != null) {
                                activity.mNewFragment = Pair.create(DrawerSelection.DATABASE_EVENTS, null);
                            }
                            return true;
                        }

                        return false;
                    // Chat
                    case "chat.qed-verein.de":
                        if (path != null && path.contains("/history")) {
                            // Chat log
                            if (activity != null) {
                                LogRequest request = LogRequest.parse(data);
                                Bundle bundle = new Bundle();
                                if (request != null) {
                                    bundle.putSerializable(LogFragment.ARGUMENT_LOG_REQUEST, request);
                                }
                                activity.mNewFragment = Pair.create(DrawerSelection.CHAT_LOG, bundle);
                            }
                        } else {
                            // Chat
                            if (activity != null) {
                                activity.mNewFragment = Pair.create(DrawerSelection.CHAT, null);
                            }
                        }
                        return true;
                    // Gallery
                    case "qedgallery.qed-verein.de":
                        if (activity != null) {
                            activity.mNewFragment = Pair.create(DrawerSelection.GALLERY, null);
                        }
                        return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (handleIntent(intent,this)) {
            if (mNewFragment != null) {
                new Handler(Looper.getMainLooper()).post(() -> reloadFragment(false));
            }
        } else {
            super.finish();
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
            return;
        }

        super.onNewIntent(intent);
    }
    //</editor-fold>

    //<editor-fold desc="ActionModes" defaultstate="collapsed">

    @Override
    public void onSupportActionModeStarted(@NonNull ActionMode mode) {
        super.onSupportActionModeStarted(mode);
        mActionMode = mode;

        var colorPrimary = new TypedValue();
        getTheme().resolveAttribute(R.attr.colorPrimary, colorPrimary, true);
        var anim = ObjectAnimator.ofArgb(getWindow(), "statusBarColor", colorPrimary.data);
        anim.setDuration(300);
        anim.start();
    }

    @Override
    public void onSupportActionModeFinished(@NonNull ActionMode mode) {
        super.onSupportActionModeFinished(mode);
        mActionMode = null;

        var colorPrimaryDark = new TypedValue();
        getTheme().resolveAttribute(R.attr.colorPrimaryDark, colorPrimaryDark, true);
        var anim = ObjectAnimator.ofArgb(getWindow(), "statusBarColor", colorPrimaryDark.data);
        anim.setDuration(300);
        anim.start();
    }

    public void finishActionMode() {
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    //</editor-fold>

    //<editor-fold desc="Drawer Callback" defaultstate="collapsed">
    @Override
    public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {}

    @Override
    public void onDrawerOpened(@NonNull View drawerView) {}

    @Override
    public void onDrawerClosed(@NonNull View drawerView) {
        if (mNewFragment != null) {
            reloadFragment(false);
        }
    }

    @Override
    public void onDrawerStateChanged(int newState) {}
    //</editor-fold>

    @Getter
    public enum DrawerSelection {
        CHAT(1, R.string.title_fragment_chat, R.id.nav_chat, ChatFragment.class, ChatFragment::newInstance),
        CHAT_LOG(2, R.string.title_fragment_log, R.id.nav_chat_log, LogFragment.class, LogFragment::newInstance),
        CHAT_DATABASE(3, R.string.title_fragment_chat_database, R.id.nav_chat_db, ChatDatabaseFragment.class, ChatDatabaseFragment::newInstance),
        DATABASE_PEOPLE(4, R.string.title_fragment_persons_database, R.id.nav_database_persons, PersonDatabaseFragment.class, PersonDatabaseFragment::newInstance),
        DATABASE_EVENTS(5, R.string.title_fragment_events_database, R.id.nav_database_events, EventDatabaseFragment.class, EventDatabaseFragment::newInstance),
        GALLERY(6, R.string.title_fragment_gallery, R.id.nav_gallery, GalleryFragment.class, GalleryFragment::newInstance);

        private final int id;
        @StringRes
        private final int title;
        @IdRes
        private final int menuItemId;

        private final Class<? extends QEDFragment> fragmentClass;
        private final Function<Integer, QEDFragment> newInstance;

        DrawerSelection(int id,
                        @StringRes int title,
                        @IdRes int menuItemId,
                        @NonNull Class<? extends QEDFragment> fragmentClass,
                        @NonNull Function<Integer, QEDFragment> newInstance) {
            this.id = id;
            this.title = title;
            this.menuItemId = menuItemId;
            this.fragmentClass = fragmentClass;
            this.newInstance = newInstance;
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

        public static DrawerSelection byClass(Class<? extends QEDFragment> clazz, DrawerSelection defaultValue) {
            if (clazz == ChatFragment.class) {
                return CHAT;
            } else if (clazz == LogFragment.class) {
                return CHAT_LOG;
            } else if (clazz == ChatDatabaseFragment.class) {
                return CHAT_DATABASE;
            } else if (clazz == PersonDatabaseFragment.class) {
                return DATABASE_PEOPLE;
            } else if (clazz == EventDatabaseFragment.class) {
                return DATABASE_EVENTS;
            } else if (clazz == GalleryFragment.class) {
                return GALLERY;
            } else {
                return defaultValue;
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
}
