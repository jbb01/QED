package com.jonahbauer.qed.activities.settings;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.databinding.ActivitySettingsBinding;

public class SettingsActivity extends AppCompatActivity {
    public static final String EXTRA_FRAGMENT_CLASS = "fragment";
    private String mFragmentClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivitySettingsBinding binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.settings_content);
        if (fragment == null) {
            mFragmentClass = getIntent().getExtras().getString(EXTRA_FRAGMENT_CLASS);
            fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                    getClassLoader(),
                    mFragmentClass
            );

            getSupportFragmentManager().beginTransaction()
                                       .replace(R.id.settings_content, fragment)
                                       .commit();
        } else {
            mFragmentClass = fragment.getClass().getName();
        }

        if (fragment instanceof PreferenceFragment && actionBar != null) {
            actionBar.setTitle(((PreferenceFragment) fragment).getTitle());
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.screenWidthDp >= 900) {
            Intent intent = new Intent();
            intent.putExtra(EXTRA_FRAGMENT_CLASS, mFragmentClass);
            setResult(0, intent);
            finish();
        }
    }
}