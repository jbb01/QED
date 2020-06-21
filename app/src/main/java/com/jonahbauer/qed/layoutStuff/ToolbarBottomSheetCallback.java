package com.jonahbauer.qed.layoutStuff;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.jonahbauer.qed.layoutStuff.viewPagerBottomSheet.ViewPagerBottomSheetBehavior;

public class ToolbarBottomSheetCallback extends BottomSheetBehavior.BottomSheetCallback {
    private final Toolbar mToolbar;
    private final ViewGroup mRest;

    private final int mToolbarHeight;
    private final Drawable mNavIcon;

    private BottomSheetDialogFragment mBottomSheetDialogFragment;
    private BottomSheetDialog mBottomSheetDialog;

    @ColorInt
    private int mBackgroundColor;

    @SuppressWarnings("unused")
    public ToolbarBottomSheetCallback(@NonNull Toolbar toolbar, @NonNull ViewGroup rest, @NonNull BottomSheetDialog bottomSheetDialog) {
        this(toolbar, rest);
        this.mBottomSheetDialog = bottomSheetDialog;
    }

    public ToolbarBottomSheetCallback(@NonNull Toolbar toolbar, @NonNull ViewGroup rest, @NonNull BottomSheetDialogFragment bottomSheetDialogFragment) {
        this(toolbar, rest);
        this.mBottomSheetDialogFragment = bottomSheetDialogFragment;
    }

    private ToolbarBottomSheetCallback(@NonNull Toolbar toolbar, @NonNull ViewGroup rest) {
        this.mToolbar = toolbar;
        this.mRest = rest;

        mToolbarHeight = toolbar.getLayoutParams().height;
        mNavIcon = toolbar.getNavigationIcon();

        TypedValue value = new TypedValue();
        toolbar.getContext().getTheme().resolveAttribute(android.R.attr.colorBackground, value, true);
        mBackgroundColor = value.data;

        rest.setTranslationY(-mToolbarHeight);
    }

    @Override
    public void onStateChanged(@NonNull View bottomSheet, int newState) {
        if (newState == ViewPagerBottomSheetBehavior.STATE_EXPANDED) {
            mToolbar.setVisibility(View.VISIBLE);
        } else if (newState == ViewPagerBottomSheetBehavior.STATE_HIDDEN) {
            if (mBottomSheetDialog != null) mBottomSheetDialog.dismiss();
            else if (mBottomSheetDialogFragment != null) mBottomSheetDialogFragment.dismiss();
        }
    }

    @Override
    public void onSlide(@NonNull View bottomSheet, float slideOffset) {
        if (slideOffset > 0) {
            float offset = (1 - slideOffset) * mToolbarHeight;
            mToolbar.setVisibility(View.VISIBLE);
            mRest.setTranslationY(-offset);

            if (slideOffset > 0.6f) {
                int rgb = (int)(- 637.5f * slideOffset + 637.5f);
                int color = Color.rgb(rgb, rgb, rgb);
                mToolbar.setTitleTextColor(color);
                mNavIcon.setTint(color);
            } else {
                mToolbar.setTitleTextColor(mBackgroundColor);
                mNavIcon.setTint(mBackgroundColor);
            }
        } else {
            mToolbar.setVisibility(View.INVISIBLE);
            mRest.setTranslationY(-mToolbarHeight);
        }
    }
}
