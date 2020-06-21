package com.jonahbauer.qed.layoutStuff;

import android.animation.ArgbEvaluator;
import android.graphics.Color;
import android.view.View;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

public class ColorfulBottomSheetCallback extends BottomSheetBehavior.BottomSheetCallback {
    private final Window mRootWindow;
    private final View mTouchOutside;
    @ColorInt private final int mColor;

    private final Interpolator mInterpolator = new AccelerateInterpolator();
    private final ArgbEvaluator mEvaluator = new ArgbEvaluator();

    private final int mOldStatusBarColor;


    public ColorfulBottomSheetCallback(@NonNull DialogFragment dialogFragment, @NonNull Window rootWindow, View touchOutside, @ColorInt int color) {
        this(rootWindow, touchOutside, color);

        dialogFragment.getLifecycle().addObserver(new LifecycleObserver() {
            @SuppressWarnings("unused")
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            public void onDestroy() {
                ColorfulBottomSheetCallback.this.onDestroy();
                dialogFragment.getLifecycle().removeObserver(this);
            }
        });
    }

    private ColorfulBottomSheetCallback(@NonNull Window rootWindow, View touchOutside, @ColorInt int color) {
        this.mRootWindow = rootWindow;
        this.mTouchOutside = touchOutside;
        this.mColor = color;
        this.mOldStatusBarColor = mRootWindow.getStatusBarColor();
    }

    @Override
    public void onSlide(@NonNull View bottomSheet, float slideOffset) {
        if (slideOffset > 0) {
            float alpha = mInterpolator.getInterpolation(slideOffset) * 128;

            int interpolatedColor = Color.argb(
                    (int)(alpha),
                    Color.red(mColor),
                    Color.green(mColor),
                    Color.blue(mColor));

            mTouchOutside.setBackgroundColor(interpolatedColor);

            if (mRootWindow != null) {
                mRootWindow.setStatusBarColor((Integer) mEvaluator.evaluate(mInterpolator.getInterpolation(slideOffset), mOldStatusBarColor, mColor));
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

    public void onDestroy() {
        if (mTouchOutside != null) mTouchOutside.setBackground(null);
        if (mRootWindow != null) mRootWindow.setStatusBarColor(mOldStatusBarColor);
    }
}
