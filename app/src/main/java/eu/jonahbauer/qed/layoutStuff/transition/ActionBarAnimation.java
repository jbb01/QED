package eu.jonahbauer.qed.layoutStuff.transition;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.transition.Transition;
import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.activities.MainActivity;

public class ActionBarAnimation implements Transition.TransitionListener {
    private int mDuration;

    private final MainActivity mActivity;
    private final @ColorInt int mActionBarColor;

    private ValueAnimator mAnimator;

    public ActionBarAnimation(Fragment fragment, @ColorInt int actionBarColor) {
        this.mActivity = ((MainActivity) fragment.requireActivity());
        this.mActionBarColor = actionBarColor;
        this.mDuration = fragment.getResources().getInteger(R.integer.material_motion_duration_long_1);
    }

    @Override
    public void onTransitionStart(@NonNull Transition transition) {
        mAnimator = ObjectAnimator.ofArgb(mActivity, "actionBarColor", mActionBarColor);
        mAnimator.setDuration(mDuration);
        mAnimator.start();
    }

    @Override
    public void onTransitionEnd(@NonNull Transition transition) {
        if (mAnimator != null) mAnimator.end();
    }

    @Override
    public void onTransitionCancel(@NonNull Transition transition) {
        if (mAnimator != null) mAnimator.cancel();
    }

    @Override
    public void onTransitionPause(@NonNull Transition transition) {
        if (mAnimator != null) mAnimator.pause();
    }

    @Override
    public void onTransitionResume(@NonNull Transition transition) {
        if (mAnimator != null) mAnimator.resume();
    }

    public void setDuration(int duration) {
        this.mDuration = duration;
    }
}
