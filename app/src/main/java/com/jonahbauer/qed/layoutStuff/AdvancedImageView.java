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

public class AdvancedImageView extends androidx.appcompat.widget.AppCompatImageView {
    private Mode mode = NONE;
    private boolean dragging;

    private final PointF start = new PointF();
    private final PointF zoomCenter = new PointF();
    private float oldDistance = 0.0f;

    private float fitScale;
    private float overfitScaleFactor;

    private final Matrix matrix = new Matrix();
    private final Matrix savedMatrix = new Matrix();

    private int viewWidth;
    private int viewHeight;
    private int srcWidth;
    private int srcHeight;

    private long lastClick;

    private boolean fit = false;

    private Animator animator;

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

        viewHeight = getMeasuredHeight();
        viewWidth = getMeasuredWidth();

        if (changed) init();
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);

        Drawable source = getDrawable();

        if (source == null) return;

        srcHeight = source.getIntrinsicHeight();
        srcWidth = source.getIntrinsicWidth();

        init();
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);

        Drawable source = getDrawable();

        if (source == null) return;

        srcHeight = source.getIntrinsicHeight();
        srcWidth = source.getIntrinsicWidth();

        init();
    }

    @Override
    public void setImageResource(@DrawableRes int resId) {
        super.setImageResource(resId);

        Drawable source = getDrawable();

        if (source == null) return;

        srcHeight = source.getIntrinsicHeight();
        srcWidth = source.getIntrinsicWidth();

        init();
    }

    private void init() {
        float scaleX = ((float) viewWidth) / ((float)srcWidth);
        float scaleY = ((float) viewHeight) / ((float)srcHeight);
        fitScale = Math.min(scaleX, scaleY);

        float overfitScale = Math.max(scaleX, scaleY);
        overfitScaleFactor = overfitScale / fitScale;

        if (animator != null) animator.cancel();

        centerAndScale(matrix, fitScale);
        setImageMatrix(matrix);

        fit = true;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: // on first down start dragging
                mode = DRAG;
                start.set(event.getX(), event.getY());
                savedMatrix.set(matrix);

                if (animator != null) animator.cancel();
                break;
            case MotionEvent.ACTION_POINTER_DOWN: // on second down start zooming
                oldDistance = spacing(event, 0 ,1);
                if (oldDistance > 10f && event.getPointerCount() == 2) {
                    mode = ZOOM;
                    savedMatrix.set(matrix);
                    midPoint(zoomCenter, event, 0 , 1);
                }

                // more than 2 fingers -> do nothing
                if (event.getPointerCount() > 2) mode = NONE;
                break;
            case MotionEvent.ACTION_POINTER_UP: // if only two fingers remain start zooming, one finger -> dragging
                if (event.getPointerCount() == 3) { // lifted pointer is still counted
                    List<Integer> indices = new ArrayList<>(Arrays.asList(0,1,2));
                    indices.remove((Integer) event.getActionIndex());

                    oldDistance = spacing(event, indices.get(0), indices.get(1));
                    if (oldDistance > 10f && event.getPointerCount() == 3) {
                        mode = ZOOM;
                        savedMatrix.set(matrix);
                        midPoint(zoomCenter, event, indices.get(0), indices.get(1));
                    }

                    if (animator != null) animator.cancel();
                } else if (event.getPointerCount() == 2) {
                    int indexStillDown;
                    if (event.getActionIndex() == 0)
                        indexStillDown = 1;
                    else
                        indexStillDown = 0;

                    mode = DRAG;
                    start.set(event.getX(indexStillDown), event.getY(indexStillDown));
                    savedMatrix.set(matrix);
                    dragging = true;

                    if (animator != null) animator.cancel();
                } else {
                    mode = NONE;
                }
                break;
            case MotionEvent.ACTION_UP:
                // if no bigger motion has been detected interpret as click
                if (mode == DRAG && !dragging) {
                    performClick();
                    break;
                }

                if (animator != null && !animator.canceled) break;

                savedMatrix.set(matrix);
                if (fixPositionAndScale(matrix)) {
                    animate(savedMatrix, matrix);
                    fit = false;
                }

                // reset
                dragging = false;
                mode = NONE;
                break;
            case MotionEvent.ACTION_MOVE:
                if (animator != null && !animator.canceled) break;

                if (mode == DRAG && dragging) {
                    // translate image
                    matrix.set(savedMatrix);
                    float tmpTranslateX = event.getX() - start.x;
                    float tmpTranslateY = event.getY() - start.y;
                    matrix.postTranslate(tmpTranslateX, tmpTranslateY);
                } else if (mode == DRAG) {
                    // start dragging only when the motion is big enough
                    if (squaredDistance(start.x, start.y, event.getX(), event.getY()) > 100) {
                        dragging = true;
                    }
                    break;
                } else if (mode == ZOOM) {
                    // scale image
                    float newDistance = spacing(event, 0 , 1);
                    if (newDistance > 10f) {
                        matrix.set(savedMatrix);
                        float tmpScale = newDistance / oldDistance;
                        matrix.postScale(tmpScale, tmpScale, zoomCenter.x, zoomCenter.y);
                    }
                }
                fit = false;
                setImageMatrix(matrix);
                break;
        }

        return true;
    }

    @Override
    public boolean performClick() {
        long time = System.currentTimeMillis();

        if (time - lastClick < 500) { // double tap
            if (animator != null) animator.cancel();

            if (!fit) {
                savedMatrix.set(matrix);
                centerAndScale(matrix, fitScale);
                animate(savedMatrix, matrix);
                fit = true;
                lastClick = -500;
            } else {
                savedMatrix.set(matrix);
                matrix.postScale(overfitScaleFactor, overfitScaleFactor, start.x, start.y);
                fixPositionAndScale(matrix);
                animate(savedMatrix, matrix);
                fit = false;
                lastClick = -500;
            }
            return true;
        } else {
            lastClick = time;
            savedMatrix.set(matrix);
            if (fixPositionAndScale(matrix)) {
                animate(savedMatrix, matrix);
                fit = false;
            }
            return super.performClick();
        }
    }

    /**
     * @return the square of the euclidian distance between the two points (x1,y1) and (x2,y2)
     */
    @Contract(pure = true)
    private float squaredDistance(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return dx * dx + dy * dy;
    }

    /**
     * @return the distance between the pointers with indices {@param index1} and {@param index2}
     */
    private float spacing(@NonNull MotionEvent event, int index1, int index2) {
        float dx = event.getX(index1) - event.getX(index2);
        float dy = event.getY(index1) - event.getY(index2);
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * calculates the mid point between the two given pointers of the motion event and stores it to {@param point}
     */
    private void midPoint(@NonNull PointF point, @NonNull MotionEvent event, int index1, int index2) {
        float x = event.getX(index1) + event.getX(index2);
        float y = event.getY(index1) + event.getY(index2);
        point.set(x / 2, y / 2);
    }

    /**
     * changes the given matrix such that it will center the image on the screen with the given scale
     */
    private void centerAndScale(@NonNull Matrix matrix, float scale) {
        float translateX = (viewWidth - srcWidth) / 2f;
        float translateY = (viewHeight - srcHeight) / 2f;
        matrix.setTranslate(translateX, translateY);
        matrix.postScale(scale, scale, viewWidth / 2f, viewHeight / 2f);
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
        if (scale < Math.min(1, fitScale)) { // scale too low
            centerAndScale(matrix, Math.min(1, fitScale));
            changed = true;
        } else if (scale < fitScale) { // pictures smaller than screen should be centered
            centerAndScale(matrix, scale);
            changed = true;
        } else if (scale == fitScale) { // pictures exactly the fitting size should only be changed if they are not correctly aligned
            if (!fit) {
                centerAndScale(matrix, scale);
                changed = true;
            }
        } else if (scale > 10 * fitScale) { // scale too high
            translateX = viewWidth / 2f - (viewWidth / 2f - translateX) * 10 * fitScale / scale;
            translateY = viewHeight / 2f - (viewHeight / 2f - translateY) * 10 * fitScale / scale;
            scale = 10 * fitScale;

            matrix.setScale(scale, scale);
            matrix.postTranslate(translateX, translateY);
            changed = true;
        }

        // translation stuff
        if (scale > fitScale) { // don't allow black strips when image is bigger than screen
            if (scale * srcWidth > viewWidth) {
                float maxXTranslation = 0;
                float minXTranslation = viewWidth - scale * srcWidth;
                if (translateX > maxXTranslation) {
                    matrix.postTranslate(maxXTranslation - translateX, 0);
                    changed = true;
                } else if (translateX < minXTranslation) {
                    matrix.postTranslate(minXTranslation - translateX, 0);
                    changed = true;
                }
            } else {
                matrix.postTranslate((viewWidth - scale * srcWidth) / 2f - translateX, 0);
                changed = true;
            }

            if (scale * srcHeight > viewHeight) {
                float maxYTranslation = 0;
                float minYTranslation = viewHeight - scale * srcHeight;
                if (translateY > maxYTranslation) {
                    matrix.postTranslate(0, maxYTranslation - translateY);
                    changed = true;
                } else if (translateY < minYTranslation) {
                    matrix.postTranslate(0, minYTranslation - translateY);
                    changed = true;
                }
            } else {
                matrix.postTranslate(0, (viewHeight - scale * srcHeight) / 2f - translateY);
                changed = true;
            }
        }

        return changed;
    }

    private void animate(@NonNull Matrix initState, @NonNull Matrix finalState) {
        if (animator != null) animator.cancel();

        animator = new Animator(initState, finalState);
        post(animator);
    }

    private class Animator implements Runnable {
        private final Interpolator interpolator;
        private final long startTime;
        private final long duration;

        private final float finalScale;
        private final float finalX;
        private final float finalY;
        
        private final float initScale;
        private final float initX;
        private final float initY;

        private boolean canceled;
        
        final Matrix target;

        private Animator(@NonNull Matrix initState, @NonNull Matrix finalState) {
            interpolator = new AccelerateDecelerateInterpolator();
            startTime = System.currentTimeMillis();
            duration = 500;

            float[] finalValues = new float[9];
            finalState.getValues(finalValues);
            finalScale = finalValues[Matrix.MSCALE_X];
            finalX = finalValues[Matrix.MTRANS_X];
            finalY = finalValues[Matrix.MTRANS_Y];
            
            float[] initValues = new float[9];
            initState.getValues(initValues);
            initScale = initValues[Matrix.MSCALE_X];
            initX = initValues[Matrix.MTRANS_X];
            initY = initValues[Matrix.MTRANS_Y];
            
            this.target = initState;
        }

        @Override
        public void run() {
            float t = (float) (System.currentTimeMillis() - startTime) / duration;
            t = t > 1.0f ? 1.0f : t;
            float interpolatedRatio = interpolator.getInterpolation(t);
            float tempScale = initScale + interpolatedRatio * (finalScale - initScale);
            float tempX = initX + interpolatedRatio * (finalX - initX);
            float tempY = initY + interpolatedRatio * (finalY - initY);

            target.reset();
            target.postScale(tempScale, tempScale);
            target.postTranslate(tempX, tempY);

            setImageMatrix(target);

            if (canceled) {
                savedMatrix.set(target);
                matrix.set(target);
                return;
            }

            if (t < 1f) {
                post(this);
            }
        }

        private void cancel() {
            canceled = true;
        }
    }

    enum Mode {
        ZOOM, DRAG, NONE
    }
}
