package com.jonahbauer.qed.activities.sheets;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.databinding.SingleFragmentBinding;

public abstract class AbstractInfoActivity extends AppCompatActivity {
    private SingleFragmentBinding mBinding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = SingleFragmentBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        // instantiate fragment
        Fragment fragment = createFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment, fragment, fragment.getTag()).commit();

        int color = getColor();
        int darkColor = Color.rgb(
                Color.red(color) * 204 / 255,
                Color.green(color) * 204 / 255,
                Color.blue(color) * 204 / 255
        );

        getWindow().setStatusBarColor(darkColor);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        View rootView = mBinding.fragment;

        // inflate new version of fragment so that layout matches orientation
        if (rootView != null) rootView.post(() -> {
            View common = rootView.findViewById(R.id.common);

            Fragment fragment = createFragment();

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment, fragment, fragment.getTag());
            transaction.addSharedElement(common, "common");
            transaction.commit();
        });
    }

    @ColorInt
    public abstract int getColor();

    public abstract AbstractInfoFragment createFragment();
}
