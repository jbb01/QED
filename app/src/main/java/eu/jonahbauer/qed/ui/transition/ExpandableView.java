package eu.jonahbauer.qed.ui.transition;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.vectordrawable.graphics.drawable.SeekableAnimatedVectorDrawable;
import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.util.ViewUtils;
import lombok.Setter;

public class ExpandableView {
    private final @NonNull View view;
    private final @NonNull ValueAnimator animator;

    @Setter
    private @Nullable SeekableAnimatedVectorDrawable drawable;

    private int targetHeight;
    private boolean reversing = false;

    public static @NonNull ExpandableView from(@NonNull View view) {
        Object tag = view.getTag(R.id.expandable);
        if (tag instanceof ExpandableView) {
            return (ExpandableView) tag;
        } else {
            var out = new ExpandableView(view);
            view.setTag(R.id.expandable, out);
            return out;
        }
    }

    private ExpandableView(@NonNull View view) {
        this.view = view;

        var duration = view.getContext().getResources().getInteger(com.google.android.material.R.integer.material_motion_duration_medium_2);
        var listener = new AnimationListener();

        this.animator = ObjectAnimator.ofFloat(0, 1);
        this.animator.setDuration(duration);
        this.animator.addListener(listener);
        this.animator.addUpdateListener(listener);
    }

    public void setExpanded(boolean expanded, boolean animate) {
        if (expanded) {
            expand(animate);
        } else {
            collapse(animate);
        }
    }

    public void expand(boolean animate) {
        if (animate && animator.getAnimatedFraction() < 1) {
            if (!animator.isRunning()) {
                animator.start();
            } else if (reversing) {
                animator.reverse();
            }
            reversing = false;
        } else {
            animator.end();
        }
    }

    public void collapse(boolean animate) {
        if (animate && animator.getAnimatedFraction() > 0) {
            animator.reverse();
            reversing = true;
        } else {
            animator.setCurrentFraction(0);
            animator.cancel();
        }
    }

    private class AnimationListener extends AnimatorListenerAdapter implements ValueAnimator.AnimatorUpdateListener {

        @Override
        public void onAnimationStart(@NonNull Animator animation) {
            // setup expanded state
            view.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            ViewUtils.setPaddingTop(view, 0);

            // measure view
            int matchParentMeasureSpec = View.MeasureSpec.makeMeasureSpec(((View) view.getParent()).getWidth(), View.MeasureSpec.EXACTLY);
            int wrapContentMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            view.measure(matchParentMeasureSpec, wrapContentMeasureSpec);

            targetHeight = view.getMeasuredHeight();
        }

        @Override
        public void onAnimationUpdate(@NonNull ValueAnimator animation) {
            var fraction = animation.getAnimatedFraction();
            if (fraction == 0) {
                // collapsed state
                view.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                view.setVisibility(View.GONE);
                view.setAlpha(1);
                ViewUtils.setPaddingTop(view, 0);
                if (drawable != null) {
                    drawable.setCurrentPlayTime(0);
                }
            } else if (fraction == 1) {
                // expanded state
                view.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                view.setVisibility(View.VISIBLE);
                view.setAlpha(1);
                ViewUtils.setPaddingTop(view, 0);
                if (drawable != null) {
                    drawable.setCurrentPlayTime(drawable.getTotalDuration());
                }
            } else {
                // intermediate states
                view.getLayoutParams().height = (int) (targetHeight * fraction);
                view.setVisibility(View.VISIBLE);
                view.setAlpha(fraction);
                ViewUtils.setPaddingTop(view, - (int) (targetHeight * (1 - fraction)));
                if (drawable != null) {
                    drawable.setCurrentPlayTime((long) (drawable.getTotalDuration() * fraction));
                }
            }
        }
    }
}
