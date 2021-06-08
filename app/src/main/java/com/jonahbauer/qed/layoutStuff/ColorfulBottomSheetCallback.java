package com.jonahbauer.qed.layoutStuff;

import android.graphics.Color;
import android.view.View;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.jonahbauer.qed.util.Colors;

public class ColorfulBottomSheetCallback extends BottomSheetBehavior.BottomSheetCallback {
    private final Window mRootWindow;
    private final View mTouchOutside;
    @ColorInt private final int mColor;

    private final Interpolator mInterpolator = new AccelerateInterpolator(1.5f);

    private final int mOldStatusBarColor;

    public ColorfulBottomSheetCallback(@NonNull Window rootWindow, View touchOutside, @ColorInt int color) {
        this.mRootWindow = rootWindow;
        this.mTouchOutside = touchOutside;
        this.mColor = color;
        this.mOldStatusBarColor = mRootWindow.getStatusBarColor();
    }

    @Override
    public void onSlide(@NonNull View bottomSheet, float slideOffset) {
        if (slideOffset > 0) {
            float alpha = mInterpolator.getInterpolation(slideOffset) * 255;

            int interpolatedColor = Color.argb(
                    (int)(alpha),
                    Color.red(mColor),
                    Color.green(mColor),
                    Color.blue(mColor));
            int darkColor = Colors.multiply(interpolatedColor, 0xFFCCCCCC);

            mTouchOutside.setBackgroundColor(interpolatedColor);

            if (mRootWindow != null) {
                mRootWindow.setStatusBarColor(darkColor);
            }
        }
    }

    @Override
    public void onStateChanged(@NonNull View bottomSheet, int newState) {
        if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
            if (mTouchOutside != null) mTouchOutside.setBackground(null);
            if (mRootWindow != null) mRootWindow.setStatusBarColor(mOldStatusBarColor);
        }
    }
}
