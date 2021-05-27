package com.jonahbauer.qed.layoutStuff;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewParent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jonahbauer.qed.Application;


/**
 * <p>
 *     An improved version of the {@code ImageView}.
 * </p>
 * <p>
 *     This version of the {@link androidx.appcompat.widget.AppCompatImageView} enables use of common
 *     gestures like pinch to zoom, dragging and double tap to fit size.
 * </p>
 */
public class AdvancedImageView extends androidx.appcompat.widget.AppCompatImageView {
    // the maximum scale at which the image fits the screen
    private float mFitScale;
    // the minimum scale at which there are no black borders, divided by mFitScale
    private float mOverfitScaleFactor;

    private final Matrix mMatrix = new Matrix();
    private final Matrix mSavedMatrix = new Matrix();

    private int mViewWidth;
    private int mViewHeight;
    private int mSrcWidth;
    private int mSrcHeight;

    private boolean mFit = false;

    private Animator mAnimator;

    private final GestureDetector mGestureDetector;
    private final GestureListener mGestureListener;
    private final ScaleGestureDetector mScaleGestureDetector;

    public AdvancedImageView(Context context) {
        this(context, null);
    }

    public AdvancedImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AdvancedImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mGestureListener = new GestureListener();
        mGestureDetector = new GestureDetector(context, mGestureListener);
        mGestureDetector.setOnDoubleTapListener(mGestureListener);
        mScaleGestureDetector = new ScaleGestureDetector(context, mGestureListener);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        mViewHeight = getMeasuredHeight();
        mViewWidth = getMeasuredWidth();

        if (changed) init();
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        init();
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
        init();
    }

    @Override
    public void setImageResource(@DrawableRes int resId) {
        super.setImageResource(resId);
        init();
    }

    /**
     * Initializes this ImageView by calculating base values and centering the image.
     *
     * These base values are {@link #mSrcHeight}, {@link #mSrcWidth}, {@link #mFitScale}, {@link #mOverfitScaleFactor}.
     */
    private void init() {
        Drawable source = getDrawable();

        if (source == null) return;

        mSrcHeight = source.getIntrinsicHeight();
        mSrcWidth = source.getIntrinsicWidth();

        float scaleX = ((float) mViewWidth) / ((float) mSrcWidth);
        float scaleY = ((float) mViewHeight) / ((float) mSrcHeight);
        mFitScale = Math.min(scaleX, scaleY);

        float overfitScale = Math.max(scaleX, scaleY);
        mOverfitScaleFactor = overfitScale / mFitScale;

        if (mAnimator != null) mAnimator.cancel();

        centerAndScale(mMatrix, mFitScale);
        setImageMatrix(mMatrix);

        setFit(true);
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        mScaleGestureDetector.onTouchEvent(event);

        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_UP) {
            mGestureListener.onUp(event);
        } else if (action == MotionEvent.ACTION_POINTER_DOWN) {
            mGestureListener.onPointerDown(event);
        } else if (action == MotionEvent.ACTION_POINTER_UP) {
            mGestureListener.onPointerUp(event);
        }

        return true;
    }

    private void setFit(boolean fit) {
        if (this.mFit != fit) {
            this.mFit = fit;

            ViewParent parent = this.getParent();
            if (parent != null) {
                parent.requestDisallowInterceptTouchEvent(!this.mFit);
            }
        }
    }

    /**
     * changes the given matrix such that it will center the image on the screen with the given scale
     */
    private void centerAndScale(@NonNull Matrix matrix, float scale) {
        float translateX = (mViewWidth - mSrcWidth) / 2f;
        float translateY = (mViewHeight - mSrcHeight) / 2f;
        matrix.setTranslate(translateX, translateY);
        matrix.postScale(scale, scale, mViewWidth / 2f, mViewHeight / 2f);
    }

    /**
     * changes the given matrix such that the transformation follows constraints:
     * - when the image is smaller than the screen it should be centered
     * - when the image is bigger than the screen there should be no black strips
     * - zooming higher than 10 times the fit-to-screen-scale is not allowed
     *
     * @return true if and only if changes were made
     */
    private boolean fixPositionAndScale(@NonNull Matrix matrix) {
        float[] values = new float[9];
        matrix.getValues(values);

        float translateX = values[Matrix.MTRANS_X];
        float translateY = values[Matrix.MTRANS_Y];
        float scale = values[Matrix.MSCALE_X];

        boolean changed = false;

        // scale stuff
        if (scale < Math.min(1, mFitScale)) { // scale too low
            centerAndScale(matrix, Math.min(1, mFitScale));
            changed = true;
        } else if (scale < mFitScale) { // pictures smaller than screen should be centered
            centerAndScale(matrix, scale);
            changed = true;
        } else if (scale == mFitScale) { // pictures exactly the fitting size should only be changed if they are not correctly aligned
            if (!mFit) {
                centerAndScale(matrix, scale);
                changed = true;
            }
        } else if (scale > 10 * mFitScale) { // scale too high
            translateX = mViewWidth / 2f - (mViewWidth / 2f - translateX) * 10 * mFitScale / scale;
            translateY = mViewHeight / 2f - (mViewHeight / 2f - translateY) * 10 * mFitScale / scale;
            scale = 10 * mFitScale;

            matrix.setScale(scale, scale);
            matrix.postTranslate(translateX, translateY);
            changed = true;
        }

        // translation stuff
        if (scale > mFitScale) { // don't allow black strips when image is bigger than screen
            if (scale * mSrcWidth > mViewWidth) {
                float maxXTranslation = 0;
                float minXTranslation = mViewWidth - scale * mSrcWidth;
                if (translateX > maxXTranslation) {
                    matrix.postTranslate(maxXTranslation - translateX, 0);
                    changed = true;
                } else if (translateX < minXTranslation) {
                    matrix.postTranslate(minXTranslation - translateX, 0);
                    changed = true;
                }
            } else {
                matrix.postTranslate((mViewWidth - scale * mSrcWidth) / 2f - translateX, 0);
                changed = true;
            }

            if (scale * mSrcHeight > mViewHeight) {
                float maxYTranslation = 0;
                float minYTranslation = mViewHeight - scale * mSrcHeight;
                if (translateY > maxYTranslation) {
                    matrix.postTranslate(0, maxYTranslation - translateY);
                    changed = true;
                } else if (translateY < minYTranslation) {
                    matrix.postTranslate(0, minYTranslation - translateY);
                    changed = true;
                }
            } else {
                matrix.postTranslate(0, (mViewHeight - scale * mSrcHeight) / 2f - translateY);
                changed = true;
            }
        }

        return changed;
    }

    /**
     * Animates the transition between {@code initState} and {@code finalState}.
     *
     * @param initState the initial image matrix
     * @param finalState the target image matrix
     */
    private void animate(@NonNull Matrix initState, @NonNull Matrix finalState) {
        if (mAnimator != null) mAnimator.cancel();

        mAnimator = new Animator(initState, finalState);
        post(mAnimator);
    }

    /**
     * Transitions the image matrix between to given values.
     */
    private class Animator implements Runnable {
        private final Interpolator mInterpolator;
        private final long mStartTime;
        private final long mDuration;

        private final float mFinalScale;
        private final float mFinalX;
        private final float mFinalY;
        
        private final float mInitScale;
        private final float mInitX;
        private final float mInitY;

        private boolean mCanceled;
        
        final Matrix mTarget;

        final Throwable creationStackTrace;

        private Animator(@NonNull Matrix initState, @NonNull Matrix finalState) {
            mInterpolator = new AccelerateDecelerateInterpolator();
            mStartTime = System.currentTimeMillis();
            mDuration = 500;

            float[] finalValues = new float[9];
            finalState.getValues(finalValues);
            mFinalScale = finalValues[Matrix.MSCALE_X];
            mFinalX = finalValues[Matrix.MTRANS_X];
            mFinalY = finalValues[Matrix.MTRANS_Y];
            
            float[] initValues = new float[9];
            initState.getValues(initValues);
            mInitScale = initValues[Matrix.MSCALE_X];
            mInitX = initValues[Matrix.MTRANS_X];
            mInitY = initValues[Matrix.MTRANS_Y];
            
            this.mTarget = initState;

            this.creationStackTrace = new Throwable();
        }

        @Override
        public void run() {
            float t = (float) (System.currentTimeMillis() - mStartTime) / mDuration;
            t = Math.min(t, 1.0f);
            float interpolatedRatio = mInterpolator.getInterpolation(t);
            float tempScale = mInitScale + interpolatedRatio * (mFinalScale - mInitScale);
            float tempX = mInitX + interpolatedRatio * (mFinalX - mInitX);
            float tempY = mInitY + interpolatedRatio * (mFinalY - mInitY);

            mTarget.reset();
            mTarget.postScale(tempScale, tempScale);
            mTarget.postTranslate(tempX, tempY);

            setImageMatrix(mTarget);

            if (mCanceled) {
                mSavedMatrix.set(mTarget);
                mMatrix.set(mTarget);
                return;
            }

            if (t < 1f) {
                post(this);
            } else {
                if (mFinalScale == mFitScale) {
                    setFit(true);
                }
            }
        }

        private void cancel() {
            mCanceled = true;
        }
    }

    @SuppressWarnings({"unused", "UnusedReturnValue"})
    private class GestureListener extends GestureDetector.SimpleOnGestureListener implements ScaleGestureDetector.OnScaleGestureListener {
        private final PointF mStart = new PointF();

        private boolean mScrolling;
        private boolean mDoubleTap;
        private float mOldScaleFactor = 1;

        @Override
        public boolean onDown(@NonNull MotionEvent e) {
            // onDown is called after onDoubleTap and animations due to double tapping should not be canceled.
            if (!mDoubleTap && mAnimator != null) mAnimator.cancel();

            mDoubleTap = false;
            mScrolling = false;
            mOldScaleFactor = 1;

            mSavedMatrix.set(mMatrix);
            mStart.set(e.getX(), e.getY());
            return true;
        }

        public boolean onUp(@NonNull MotionEvent e) {
            if (mScrolling) {
                mSavedMatrix.set(mMatrix);
                if (fixPositionAndScale(mMatrix)) {
                    animate(mSavedMatrix, mMatrix);
                    setFit(false);
                }
            }
            return true;
        }

        public boolean onPointerDown(@NonNull MotionEvent e) {
            if (e.getPointerCount() > 1) {
                // this triggers requestDisallowInterceptTouchEvent
                setFit(false);
                return true;
            }
            return false;
        }

        public boolean onPointerUp(@NonNull MotionEvent e) {return false;}

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (mFit) return false;

            mScrolling = true;

            mMatrix.postTranslate(-distanceX, -distanceY);
            setFit(false);
            setImageMatrix(mMatrix);
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return AdvancedImageView.super.performClick();
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (mAnimator != null) mAnimator.cancel();

            mDoubleTap = true;

            if (!mFit) {
                mSavedMatrix.set(mMatrix);
                centerAndScale(mMatrix, mFitScale);
                animate(mSavedMatrix, mMatrix);
                setFit(true);
            } else {
                mSavedMatrix.set(mMatrix);
                mMatrix.postScale(mOverfitScaleFactor, mOverfitScaleFactor, mStart.x, mStart.y);
                fixPositionAndScale(mMatrix);
                animate(mSavedMatrix, mMatrix);
                setFit(false);
            }
            return true;
        }

        @Override
        public boolean onScale(@NonNull ScaleGestureDetector detector) {
            Log.d(Application.LOG_TAG_DEBUG, "onTouchEvent: scaling: " + detector.getPreviousSpan());

            float scaleFactor = detector.getScaleFactor() / mOldScaleFactor;
            mOldScaleFactor = detector.getScaleFactor();

            mMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
            setFit(false);
            setImageMatrix(mMatrix);
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
        }
    }
}
