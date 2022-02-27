package com.jonahbauer.qed.layoutStuff.transition;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.transition.Transition;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.MainActivity;

public class ActionBarAnimation implements Transition.TransitionListener {
    private int mDuration;

    private final MainActivity mActivity;
    private final @ColorInt int mActionBarColor;
    private final boolean mVisible;
    private final @ColorInt int mBackgroundColor;

    private ValueAnimator mAnimator;
    private ValueAnimator mAnimatorAlpha;

    public ActionBarAnimation(Fragment fragment, @ColorInt int actionBarColor) {
        this(fragment, actionBarColor, true);
    }

    public ActionBarAnimation(Fragment fragment, @ColorInt int actionBarColor, boolean visible) {
        this(fragment, actionBarColor, visible, Color.TRANSPARENT);
    }

    public ActionBarAnimation(Fragment fragment, @ColorInt int actionBarColor, boolean visible, @ColorInt int backgroundColor) {
        this.mActivity = ((MainActivity) fragment.requireActivity());
        this.mActionBarColor = actionBarColor;
        this.mVisible = visible;
        this.mBackgroundColor = backgroundColor;
        this.mDuration = fragment.getResources().getInteger(R.integer.material_motion_duration_long_1);
    }

    @Override
    public void onTransitionStart(@NonNull Transition transition) {
        mAnimator = ObjectAnimator.ofArgb(mActivity, "actionBarColor", mActionBarColor);
        mAnimator.setDuration(mDuration);
        mAnimator.start();

        mAnimatorAlpha = ObjectAnimator.ofFloat(mActivity, "actionBarAlpha", mVisible ? 1 : 0);
        mAnimatorAlpha.setDuration(mDuration);
        mAnimatorAlpha.start();

        mActivity.setBackgroundColor(mBackgroundColor);

        if (mVisible) {
            mActivity.getSupportActionBar().show();
        }
    }

    @Override
    public void onTransitionEnd(@NonNull Transition transition) {
        if (mAnimator != null) mAnimator.end();
        if (mAnimatorAlpha != null) mAnimatorAlpha.end();
        if (!mVisible) {
            mActivity.getSupportActionBar().hide();
        }

        mActivity.setBackgroundColor(Color.TRANSPARENT);
    }

    @Override
    public void onTransitionCancel(@NonNull Transition transition) {
        if (mAnimator != null) mAnimator.cancel();
        if (mAnimatorAlpha != null) mAnimatorAlpha.cancel();

        mActivity.setBackgroundColor(Color.TRANSPARENT);
    }

    @Override
    public void onTransitionPause(@NonNull Transition transition) {
        if (mAnimator != null) mAnimator.pause();
        if (mAnimatorAlpha != null) mAnimatorAlpha.pause();
    }

    @Override
    public void onTransitionResume(@NonNull Transition transition) {
        if (mAnimator != null) mAnimator.resume();
        if (mAnimatorAlpha != null) mAnimatorAlpha.resume();
    }

    public void setDuration(int duration) {
        this.mDuration = duration;
    }
}
