package com.jonahbauer.qed.activities.settings;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.databinding.ActivityRootSettingsBinding;

public class RootSettingsActivity extends AppCompatActivity implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback  {
    private static final String ARG_FRAGMENT_CLASS = "fragment";

    private boolean mTwoPane;
    private String mVisibleFragment;

    private ActivityResultLauncher<Intent> mSettingsLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityRootSettingsBinding binding = ActivityRootSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (binding.settingsContent != null) {
            mTwoPane = true;
        }

        mSettingsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                (ActivityResult result) -> {
                    Intent data = result.getData();
                    if (mTwoPane && data != null && data.hasExtra(SettingsActivity.EXTRA_FRAGMENT_CLASS)) {
                        String fragmentClass = data.getStringExtra(SettingsActivity.EXTRA_FRAGMENT_CLASS);
                        showFragment(fragmentClass, null);
                    }
                }
        );


        if (savedInstanceState != null) {
            String fragmentClass = savedInstanceState.getString(ARG_FRAGMENT_CLASS, null);
            if (fragmentClass != null) {
                if (mTwoPane) {
                    showFragment(fragmentClass, null);
                } else {
                    launchActivity(fragmentClass);
                }
            }
        }
    }

    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, @NonNull Preference pref) {
        Bundle args = pref.getExtras();
        String fragmentClass = pref.getFragment();

        if (mTwoPane) {
            showFragment(fragmentClass, args);
        } else {
            launchActivity(fragmentClass);
        }

        return true;
    }

    private void showFragment(String fragmentClass, @Nullable Bundle args) {
        Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                fragmentClass
        );
        fragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                                   .replace(R.id.settings_content, fragment)
                                   .commit();

        mVisibleFragment = fragmentClass;
    }

    private void launchActivity(String fragmentClass) {
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtra(SettingsActivity.EXTRA_FRAGMENT_CLASS, fragmentClass);
        mSettingsLauncher.launch(intent);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_FRAGMENT_CLASS, mVisibleFragment);
    }
}
