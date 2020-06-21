package com.jonahbauer.qed.layoutStuff;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.jonahbauer.qed.layoutStuff.AdvancedImageView.Mode.DRAG;
import static com.jonahbauer.qed.layoutStuff.AdvancedImageView.Mode.NONE;
import static com.jonahbauer.qed.layoutStuff.AdvancedImageView.Mode.ZOOM;

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
    private Mode mMode = NONE;
    private boolean mDragging;

    private final PointF mStart = new PointF();
    private final PointF mZoomCenter = new PointF();
    private float mOldDistance = 0.0f;

    private float mFitScale;
    private float mOverfitScaleFactor;

    private final Matrix mMatrix = new Matrix();
    private final Matrix mSavedMatrix = new Matrix();

    private int mViewWidth;
    private int mViewHeight;
    private int mSrcWidth;
    private int mSrcHeight;

    private long mLastClick;

    private boolean mFit = false;

    private Animator mAnimator;

    public AdvancedImageView(Context context) {
        super(context);
    }

    public AdvancedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AdvancedImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
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

        Drawable source = getDrawable();

        if (source == null) return;

        mSrcHeight = source.getIntrinsicHeight();
        mSrcWidth = source.getIntrinsicWidth();

        init();
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);

        Drawable source = getDrawable();

        if (source == null) return;

        mSrcHeight = source.getIntrinsicHeight();
        mSrcWidth = source.getIntrinsicWidth();

        init();
    }

    @Override
    public void setImageResource(@DrawableRes int resId) {
        super.setImageResource(resId);

        Drawable source = getDrawable();

        if (source == null) return;

        mSrcHeight = source.getIntrinsicHeight();
        mSrcWidth = source.getIntrinsicWidth();

        init();
    }

    private void init() {
        float scaleX = ((float) mViewWidth) / ((float) mSrcWidth);
        float scaleY = ((float) mViewHeight) / ((float) mSrcHeight);
        mFitScale = Math.min(scaleX, scaleY);

        float overfitScale = Math.max(scaleX, scaleY);
        mOverfitScaleFactor = overfitScale / mFitScale;

        if (mAnimator != null) mAnimator.cancel();

        centerAndScale(mMatrix, mFitScale);
        setImageMatrix(mMatrix);

        mFit = true;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: // on first down start dragging
                mMode = DRAG;
                mStart.set(event.getX(), event.getY());
                mSavedMatrix.set(mMatrix);

                if (mAnimator != null) mAnimator.cancel();
                break;
            case MotionEvent.ACTION_POINTER_DOWN: // on second down start zooming
                mOldDistance = spacing(event, 0 ,1);
                if (mOldDistance > 10f && event.getPointerCount() == 2) {
                    mMode = ZOOM;
                    mSavedMatrix.set(mMatrix);
                    midPoint(mZoomCenter, event, 0 , 1);
                }

                // more than 2 fingers -> do nothing
                if (event.getPointerCount() > 2) mMode = NONE;
                break;
            case MotionEvent.ACTION_POINTER_UP: // if only two fingers remain start zooming, one finger -> dragging
                if (event.getPointerCount() == 3) { // lifted pointer is still counted
                    List<Integer> indices = new ArrayList<>(Arrays.asList(0,1,2));
                    indices.remove((Integer) event.getActionIndex());

                    mOldDistance = spacing(event, indices.get(0), indices.get(1));
                    if (mOldDistance > 10f && event.getPointerCount() == 3) {
                        mMode = ZOOM;
                        mSavedMatrix.set(mMatrix);
                        midPoint(mZoomCenter, event, indices.get(0), indices.get(1));
                    }

                    if (mAnimator != null) mAnimator.cancel();
                } else if (event.getPointerCount() == 2) {
                    int indexStillDown;
                    if (event.getActionIndex() == 0)
                        indexStillDown = 1;
                    else
                        indexStillDown = 0;

                    mMode = DRAG;
                    mStart.set(event.getX(indexStillDown), event.getY(indexStillDown));
                    mSavedMatrix.set(mMatrix);
                    mDragging = true;

                    if (mAnimator != null) mAnimator.cancel();
                } else {
                    mMode = NONE;
                }
                break;
            case MotionEvent.ACTION_UP:
                // if no bigger motion has been detected interpret as click
                if (mMode == DRAG && !mDragging) {
                    performClick();
                    break;
                }

                if (mAnimator != null && !mAnimator.mCanceled) break;

                mSavedMatrix.set(mMatrix);
                if (fixPositionAndScale(mMatrix)) {
                    animate(mSavedMatrix, mMatrix);
                    mFit = false;
                }

                // reset
                mDragging = false;
                mMode = NONE;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mAnimator != null && !mAnimator.mCanceled) break;

                if (mMode == DRAG && mDragging) {
                    // translate image
                    mMatrix.set(mSavedMatrix);
                    float tmpTranslateX = event.getX() - mStart.x;
                    float tmpTranslateY = event.getY() - mStart.y;
                    mMatrix.postTranslate(tmpTranslateX, tmpTranslateY);
                } else if (mMode == DRAG) {
                    // start dragging only when the motion is big enough
                    if (squaredDistance(mStart.x, mStart.y, event.getX(), event.getY()) > 100) {
                        mDragging = true;
                    }
                    break;
                } else if (mMode == ZOOM) {
                    // scale image
                    float newDistance = spacing(event, 0 , 1);
                    if (newDistance > 10f) {
                        mMatrix.set(mSavedMatrix);
                        float tmpScale = newDistance / mOldDistance;
                        mMatrix.postScale(tmpScale, tmpScale, mZoomCenter.x, mZoomCenter.y);
                    }
                }
                mFit = false;
                setImageMatrix(mMatrix);
                break;
        }

        return true;
    }

    @Override
    public boolean performClick() {
        long time = System.currentTimeMillis();

        if (time - mLastClick < 500) { // double tap
            if (mAnimator != null) mAnimator.cancel();

            if (!mFit) {
                mSavedMatrix.set(mMatrix);
                centerAndScale(mMatrix, mFitScale);
                animate(mSavedMatrix, mMatrix);
                mFit = true;
                mLastClick = -500;
            } else {
                mSavedMatrix.set(mMatrix);
                mMatrix.postScale(mOverfitScaleFactor, mOverfitScaleFactor, mStart.x, mStart.y);
                fixPositionAndScale(mMatrix);
                animate(mSavedMatrix, mMatrix);
                mFit = false;
                mLastClick = -500;
            }
            return true;
        } else {
            mLastClick = time;
            mSavedMatrix.set(mMatrix);
            if (fixPositionAndScale(mMatrix)) {
                animate(mSavedMatrix, mMatrix);
                mFit = false;
            }
            return super.performClick();
        }
    }

    /**
     * @return the square of the euclidian distance between the two points (x1,y1) and (x2,y2)
     */
    @Contract(pure = true)
    private static float squaredDistance(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return dx * dx + dy * dy;
    }

    /**
     * @return the distance between the pointers with indices {@param index1} and {@param index2}
     */
    private static float spacing(@NonNull MotionEvent event, int index1, int index2) {
        float dx = event.getX(index1) - event.getX(index2);
        float dy = event.getY(index1) - event.getY(index2);
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * calculates the mid point between the two given pointers of the motion event and stores it to {@param point}
     */
    private static void midPoint(@NonNull final PointF point, @NonNull MotionEvent event, int index1, int index2) {
        float x = event.getX(index1) + event.getX(index2);
        float y = event.getY(index1) + event.getY(index2);
        point.set(x / 2, y / 2);
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
            }
        }

        private void cancel() {
            mCanceled = true;
        }
    }

    enum Mode {
        ZOOM, DRAG, NONE
    }
}
