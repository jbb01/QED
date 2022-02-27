package com.jonahbauer.qed.layoutStuff.transition;

import android.animation.Animator;
import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.transition.ChangeImageTransform;
import androidx.transition.TransitionValues;

public class SizeAwareChangeImageTransition extends ChangeImageTransform {
    private static final String PROPNAME_MATRIX = "android:changeImageTransform:matrix";

    @SuppressWarnings("unused")
    public SizeAwareChangeImageTransition() {
        super();
    }

    @SuppressWarnings("unused")
    public SizeAwareChangeImageTransition(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public Animator createAnimator(@NonNull ViewGroup sceneRoot, TransitionValues startValues, TransitionValues endValues) {
        if (startValues == null || endValues == null) {
            return null;
        }

        var drawableStart = ((ImageView) startValues.view).getDrawable();
        var drawableEnd = ((ImageView) endValues.view).getDrawable();

        if (drawableStart == null || drawableEnd == null) {
            return null;
        }

        var dwidthStart = drawableStart.getIntrinsicWidth();
        var dheightStart = drawableStart.getIntrinsicHeight();
        var dwidthEnd = drawableEnd.getIntrinsicWidth();
        var dheightEnd = drawableEnd.getIntrinsicHeight();

        float scaleX = dwidthStart / (float) dwidthEnd;
        float scaleY = dheightStart / (float) dheightEnd;

        var matrixStart = (Matrix) startValues.values.get(PROPNAME_MATRIX);
        if (matrixStart != null) {
            matrixStart.preScale(scaleX, scaleY);
        }

        return super.createAnimator(sceneRoot, startValues, endValues);
    }
}
