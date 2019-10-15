package com.jonahbauer.qed.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

import com.jonahbauer.qed.Internet;
import com.jonahbauer.qed.R;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, Internet {
    DrawerLayout drawerLayout;
    NavigationView navView;
    Toolbar toolbar;
    SharedPreferences sharedPreferences;

    boolean doubleBackToExitPressedOnce;

    Fragment fragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getSharedPreferences(getString(R.string.preferences_shared_preferences), MODE_PRIVATE);

        if (!sharedPreferences.getBoolean(getString(R.string.preferences_loggedIn_key),false)) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);

        navView.setNavigationItemSelectedListener(this);

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
    public void onBackPressed() {
        // Close drawer if open
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawers();
            return;
        }

        // Require double back to exit
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, R.string.double_back_to_exit_toast, Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
            default:
                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment);
                if (fragment != null) return fragment.onOptionsItemSelected(item);
                else return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.nav_settings: {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true; }
            case R.id.nav_logout: {
                sharedPreferences.edit()
                        .putBoolean(getString(R.string.preferences_loggedIn_key), false)
                        .putString(getString(R.string.data_pwhash_key), null)
                        .putString(getString(R.string.data_userid_key), null)
                        .apply();
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                finish();
                return true; }
            case R.id.nav_gallery:
                Toast.makeText(this, R.string.coming_soon, Toast.LENGTH_SHORT).show();
                return false;
        }

        navView.setCheckedItem(menuItem);
        if (sharedPreferences.getInt(getString(R.string.preferences_drawerSelection_key),R.id.nav_chat) != menuItem.getItemId()) {
            sharedPreferences.edit()
                    .putInt(getString(R.string.preferences_drawerSelection_key), menuItem.getItemId()).apply();
            reloadFragment(false);
        }
        drawerLayout.closeDrawers();

        return true;
    }

    public void reloadFragment(boolean onlyTitle) {
         FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        switch (sharedPreferences.getInt(getString(R.string.preferences_drawerSelection_key),R.id.nav_chat)) {
            case R.id.nav_chat:
                if (!onlyTitle) {
                    fragment = new ChatFragment();
                    transaction.replace(R.id.fragment, fragment);
                }
                toolbar.setTitle(getString(R.string.title_fragment_chat));
                break;
            case R.id.nav_chat_db:
                if (!onlyTitle) {
                    fragment = new ChatDatabaseFragment();
                    transaction.replace(R.id.fragment, fragment);
                }
                toolbar.setTitle(getString(R.string.title_fragment_chat_database));
                break;
            case R.id.nav_database_persons:
                if (!onlyTitle) {
                    fragment = new PersonDatabaseFragment();
                    transaction.replace(R.id.fragment, fragment);
                }
                toolbar.setTitle(getString(R.string.title_fragment_persons_database));
                break;
            case R.id.nav_chat_log:
                if (!onlyTitle) {
                    fragment = LogFragment.newInstance(LogFragment.LOG_MODE_RECENT_DATE, "", "86400");
                    transaction.replace(R.id.fragment, fragment);
                }
                toolbar.setTitle(getString(R.string.title_fragment_log));
                break;
            case R.id.nav_database_events:
                if (!onlyTitle) {
                    fragment = new EventDatabaseFragment();
                    transaction.replace(R.id.fragment, fragment);
                }
                toolbar.setTitle(getString(R.string.title_fragment_events_database));
                break;
        }
        if (!onlyTitle) transaction.commit();
    }

    @Override
    public void onConnectionFail() {
        if (fragment != null && fragment instanceof Internet) ((Internet) fragment).onConnectionFail();
    }

    @Override
    public void onConnectionRegain() {
        if (fragment != null && fragment instanceof Internet) ((Internet) fragment).onConnectionRegain();
    }
}
