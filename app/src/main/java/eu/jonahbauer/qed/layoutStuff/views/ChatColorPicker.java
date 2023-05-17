package eu.jonahbauer.qed.layoutStuff.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.StyleRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.math.MathUtils;

import eu.jonahbauer.qed.R;

@SuppressWarnings({"unused", "SameParameterValue"})
public class ChatColorPicker extends View {
    private static final @ColorInt int[] COLORS = { Color.RED, Color.MAGENTA, Color.BLUE, Color.CYAN, Color.GREEN, Color.YELLOW, Color.RED };
    private static final float[] COLOR_POSITIONS = { 0, 0.166f, 0.333f, 0.5f, 0.666f, 0.833f, 1f };

    private final Paint mHuePaint;
    private final Paint mSaturationPaint;
    private final Paint mValuePaint;

    private final Path mTriangle;

    private float mHue;
    private float mSaturation;
    private float mValue;

    private @Px float mRingWidth;

    private Drawable mHueCursor;
    private ColorStateList mHueCursorTint;

    private Drawable mSatValCursor;
    private ColorStateList mSatValCursorTint;

    // cached values
    private float mCx;
    private float mCy;
    private float mROut;
    private float mRIn;

    private Boolean mHueTouched;

    private OnColorChangedListener mOnColorChangedListener;

    public ChatColorPicker(@NonNull Context context) {
        this(context, null);
    }

    public ChatColorPicker(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.chatColorPickerStyle);
    }

    public ChatColorPicker(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        this(context, attrs, defStyleAttr, R.style.Widget_App_ChatColorPicker);
    }

    public ChatColorPicker(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        mHuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mHuePaint.setStyle(Paint.Style.STROKE);

        mValuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mSaturationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSaturationPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));

        mTriangle = new Path();

        setLayerType(LAYER_TYPE_HARDWARE, null);

        var array = context.obtainStyledAttributes(attrs, R.styleable.ChatColorPicker, defStyleAttr, defStyleRes);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveAttributeDataForStyleable(context, R.styleable.ChatColorPicker, attrs, array, defStyleAttr, defStyleRes);
        }

        if (array.hasValue(R.styleable.ChatColorPicker_hueCursor)) {
            setHueCursor(array.getDrawable(R.styleable.ChatColorPicker_hueCursor));
        }

        if (array.hasValue(R.styleable.ChatColorPicker_hueCursorTint)) {
            setHueCursorTint(array.getColorStateList(R.styleable.ChatColorPicker_hueCursorTint));
        }

        if (array.hasValue(R.styleable.ChatColorPicker_satValCursor)) {
            setSatValCursor(array.getDrawable(R.styleable.ChatColorPicker_satValCursor));
        }

        if (array.hasValue(R.styleable.ChatColorPicker_satValCursorTint)) {
            setSatValCursorTint(array.getColorStateList(R.styleable.ChatColorPicker_satValCursorTint));
        }

        if (array.hasValue(R.styleable.ChatColorPicker_ringWidth)) {
            setRingWidth(array.getDimensionPixelSize(R.styleable.ChatColorPicker_ringWidth, 0));
        }

        if (array.hasValue(R.styleable.ChatColorPicker_hue)) {
            setHue(array.getFloat(R.styleable.ChatColorPicker_hue, 0));
        }

        if (array.hasValue(R.styleable.ChatColorPicker_saturation)) {
            setSaturation(array.getFloat(R.styleable.ChatColorPicker_saturation, 0));
        }

        if (array.hasValue(R.styleable.ChatColorPicker_value)) {
            setValue(array.getFloat(R.styleable.ChatColorPicker_value, 0));
        }

        array.recycle();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        updateSizes();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            var size = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);
            setMeasuredDimension(size - getPaddingTop() - getPaddingBottom() + getPaddingLeft() + getPaddingRight(), size);
        } else if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            var size = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
            setMeasuredDimension(size, size - getPaddingLeft() - getPaddingRight() + getPaddingTop() + getPaddingBottom());
        } else {
            var width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
            var height = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);
            var size = Math.min(width - getPaddingLeft() - getPaddingRight(), height - getPaddingTop() - getPaddingBottom());
            setMeasuredDimension(size + getPaddingLeft() + getPaddingRight(), size + getPaddingTop() + getPaddingBottom());
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        onDrawBackground(canvas, mCx, mCy, mROut);
        onDrawCursors(canvas, mCx, mCy, mROut);
    }

    private void onDrawBackground(Canvas canvas, float cx, float cy, float r) {
        canvas.drawPath(mTriangle, mValuePaint);
        canvas.drawPath(mTriangle, mSaturationPaint);
        canvas.drawCircle(cx, cy, r - mRingWidth / 2, mHuePaint);
    }

    private void onDrawCursors(Canvas canvas, float cx, float cy, float r) {
        if (mHueCursor != null) {
            var width = mHueCursor.getIntrinsicWidth();
            var height = mHueCursor.getIntrinsicHeight();

            canvas.save();
            canvas.rotate(90 - mHue, cx, cy);
            mHueCursor.setBounds(
                    (int) (cx - width / 2f),
                    (int) (cy - r - height),
                    (int) (cx + width / 2f),
                    (int) (cy - r)
            );
            mHueCursor.setTintList(mHueCursorTint);
            mHueCursor.draw(canvas);
            canvas.restore();
        }

        if (mSatValCursor != null) {
            var rIn = r - mRingWidth;
            var width = mSatValCursor.getIntrinsicWidth();
            var height = mSatValCursor.getIntrinsicHeight();

            var relSat = 1.6451613f * mSaturation - 1;
            var relVal = 1.6451613f * mValue - 0.6451613f;

            var x = cx + 0.8660254f * rIn * (2 * relVal - relSat - 2);
            var y = cy - rIn * (1 + 1.5f * relSat);

            mSatValCursor.setBounds(0, 0, width, height);
            mSatValCursor.setTintList(mSatValCursorTint);

            canvas.save();
            canvas.translate(x - width / 2f, y - height / 2f);
            mSatValCursor.draw(canvas);
            canvas.restore();
        }
    }

    private void updateSizes() {
        var width = getWidth() - getPaddingLeft() - getPaddingRight();
        var height = getHeight() - getPaddingTop() - getPaddingBottom();
        mCx = getPaddingLeft() + width / 2f;
        mCy = getPaddingTop() + height / 2f;
        mROut = Math.min(width, height) / 2f;
        if (mHueCursor != null) {
            mROut -= mHueCursor.getIntrinsicHeight();
        }
        mRIn = mROut - mRingWidth;

        mTriangle.reset();
        mTriangle.moveTo(mCx, mCy - mRIn);
        mTriangle.lineTo(mCx + 0.8660254f * mRIn, mCy + 0.5f * mRIn);
        mTriangle.lineTo(mCx - 0.8660254f * mRIn, mCy + 0.5f * mRIn);
        mTriangle.cubicTo(
                mCx - 0.9600369f * mRIn, mCy - 0.256538f * mRIn,
                mCx - 0.7021869f * mRIn, mCy - 0.7631473f * mRIn,
                mCx, mCy - mRIn
        );
        mTriangle.close();

        updateShaders();
    }

    private void updateShaders() {
        mHuePaint.setShader(new SweepGradient(mCx, mCy, COLORS, COLOR_POSITIONS));
        mHuePaint.setStrokeWidth(mRingWidth);

        mValuePaint.setShader(new LinearGradient(
                mCx - 0.8660254f * mRIn, mCy + mRIn / 2,
                mCx + 0.4330127f * mRIn, mCy - mRIn / 4,
                Color.HSVToColor(new float[] {mHue, 0, 0.39215686f}),
                Color.HSVToColor(new float[] {mHue, 0, 1}),
                Shader.TileMode.CLAMP
        ));

        mSaturationPaint.setShader(new LinearGradient(
                mCx, mCy + mRIn / 2,
                mCx, mCy - mRIn,
                Color.HSVToColor(new float[] {mHue, 0, 1}),
                Color.HSVToColor(new float[] {mHue, 0.60784316f, 1}),
                Shader.TileMode.CLAMP
        ));
        invalidate();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        var state = getDrawableState();
        if (mHueCursor != null) mHueCursor.setState(state);
        if (mSatValCursor != null) mSatValCursor.setState(state);
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(MotionEvent event) {
        var eventX = event.getX();
        var eventY = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                var x = (eventX - mCx) / mRIn;
                var y = (eventY - mCy) / mRIn;

                if (mHueTouched == null) {
                    mHueTouched = x * x + y * y > 1;
                }

                if (mHueTouched) {
                    // hue
                    var hueRadians = (float) Math.atan2(-y, x);
                    setHue(hueRadians * 57.29578f, true);
                } else {
                    float[] sv = new float[2];
                    getSaturationAndValueByCoords(x, y, sv);

                    if (sv[1] > 1 && x > 0) {
                        clampTop(sv);
                    } else if (sv[0] < 0 && x > -0.8660254f) {
                        clampBottom(sv);
                    } else if (sv[0] > 1 || sv[1] < 0 || (1 - sv[0]) * sv[1] < 0.39215686f) {
                        var approxValue = approximateClosestPointOnCurve(sv[0], sv[1]);
                        var approxSaturation = 1 - 0.39215686f / approxValue;
                        if (approxValue == 1) {
                            clampTop(sv);
                        } else if (approxSaturation < 0) {
                            clampBottom(sv);
                        } else {
                            sv[0] = approxSaturation;
                            sv[1] = approxValue;
                        }
                    }

                    setSaturationAndValue(sv[0], sv[1], true);
                }

                break;
            case MotionEvent.ACTION_UP:
                mHueTouched = null;
                break;
        }

        return true;
    }

    /**
     * Performs a coordinate transformation from xy coordinates to sv coordinates.
     * The xy coordinates have their origin in the barycenter of the sv triangle and the top tip (s = 155/255, v = 1)
     * of the triangle at (-1|0).
     */
    private void getSaturationAndValueByCoords(float x, float y, float[] sv) {
        // s = 1 / 153 * (-62 * y + 31)
        // v = 1 / 153 * (31 * sqrt(3) * x - 31 * y + 122)
        sv[0] = -0.40522876f * y + 0.20261438f;
        sv[1] = 0.3509384f * x - 0.20261438f * y + 0.79738563f;
    }

    /**
     * Maps a point to the closest point (in terms of euclidean distance in x and y) on the top right side
     * of the sv triangle (where v = 1).
     */
    private void clampTop(float[] sv) {
        sv[0] -= (sv[1] - 1) * 0.57735025f;
        sv[1] = 1;
        sv[0] = MathUtils.clamp(sv[0], 0, 0.60784316f);
    }

    /**
     * Maps a point to the closest point (in terms of euclidean distance in x and y) on the bottom side of the
     * sv triangle (where s = 0).
     */
    private void clampBottom(float[] sv) {
        sv[1] -= 0.57735025f * sv[0];
        sv[0] = 0;
        sv[1] = MathUtils.clamp(sv[1], 0.39215686f, 1);
    }

    /**
     * Calculates the value of the closest point (in terms of euclidean distance in x and y) on the extended
     * top left side of the sv triangle, i.e. the curve where {@code v * (1 - s) = 0.39215686f} with {@code 0 <= v <= 1}.
     */
    private float approximateClosestPointOnCurve(float saturation, float value) {
        // to calculate the distance in x and y of two points given in s and v we can
        // use the following formula
        //     d(s_1, v_1, s_2, v_2) = 255 / 155 * sqrt(3) * sqrt((s_1 - s_2)^2 + (v_1 - v_2)^2 - (s_1 - s_2)(v_1 - v_2))
        // plugging the given sv point and a point on the curve in d and transforming the result yields
        //     f(v) = 2 * v^4 + b * v^3 + d * v - 0.307574f = 0
        var b = (-2 * value + saturation - 1);
        var d = 0.39215686f * (-2 * saturation + value + 2);

        // when f(1) < 0 the root would be outside the allowed interval
        if (2 + b + d - 0.307574f < 0) {
            return 1;
        }

        // search for the root of f in the interval [0,1]
        var lowerBound = 0f;
        var upperBound = 1f;
        while (upperBound - lowerBound > 0.001) {
            var mid = (lowerBound + upperBound) / 2f;
            var y =  ((2 * mid + b) * mid * mid + d) * mid - 0.307574f;
            if (y > 0) {
                upperBound = mid;
            } else if (y < 0) {
                lowerBound = mid;
            } else {
                return mid;
            }
        }

        return (lowerBound + upperBound) / 2f;
    }

    private void notifyListener(boolean fromUser) {
        if (mOnColorChangedListener != null) {
            mOnColorChangedListener.onColorChanged(mHue, mSaturation, mValue, fromUser);
        }
    }

    //<editor-fold desc="Getter/Setter">
    public float getHue() {
        return mHue;
    }

    public void setHue(float hue) {
        setHue(hue, false);
    }

    private void setHue(float hue, boolean fromUser) {
        hue = (hue % 360 + 360) % 360;
        if (mHue != hue) {
            mHue = hue;
            updateShaders();
            invalidate();
            notifyListener(fromUser);
        }
    }

    public float getSaturation() {
        return mSaturation;
    }

    public void setSaturation(float saturation) {
        setSaturation(saturation, false);
    }

    private void setSaturation(float saturation, boolean fromUser) {
        saturation = MathUtils.clamp(saturation, 0, 1);
        if (mSaturation != saturation) {
            mSaturation = saturation;
            invalidate();
            notifyListener(fromUser);
        }
    }

    public float getValue() {
        return mValue;
    }

    public void setValue(float value) {
        setValue(value, false);
    }

    private void setValue(float value, boolean fromUser) {
        value = MathUtils.clamp(value, 0, 1);
        if (mValue != value) {
            mValue = value;
            invalidate();
            notifyListener(fromUser);
        }
    }

    public void setSaturationAndValue(float saturation, float value) {
        setSaturationAndValue(saturation, value, false);
    }

    private void setSaturationAndValue(float saturation, float value, boolean fromUser) {
        saturation = MathUtils.clamp(saturation, 0, 1);
        value = MathUtils.clamp(value, 0, 1);
        if (mSaturation != saturation || mValue != value) {
            mSaturation = saturation;
            mValue = value;
            invalidate();
            notifyListener(fromUser);
        }
    }

    public void setColor(float[] hsv) {
        setColor(hsv, false);
    }

    private void setColor(float[] hsv, boolean fromUser) {
        hsv[0] = (hsv[0] % 360 + 360) % 360;
        hsv[1] = MathUtils.clamp(hsv[1], 0, 1);
        hsv[2] = MathUtils.clamp(hsv[2], 0, 1);
        if (mHue != hsv[0] || mSaturation != hsv[1] || mValue != hsv[2]) {
            mHue = hsv[0];
            mSaturation = hsv[1];
            mValue = hsv[2];
            updateShaders();
            invalidate();
            notifyListener(fromUser);
        }
    }
    
    public @Px float getRingWidth() {
        return mRingWidth;
    }
    
    public void setRingWidth(@Px float ringWidth) {
        if (mRingWidth != ringWidth) {
            mRingWidth = ringWidth;
            updateSizes();
            invalidate();
        }
    }
    
    public void setHueCursor(Drawable cursor) {
        this.mHueCursor = cursor;
        if (this.mHueCursor != null) mHueCursor.setState(getDrawableState());
        updateSizes();
        invalidate();
    }
    
    public void setHueCursor(@DrawableRes int cursor) {
        setHueCursor(AppCompatResources.getDrawable(getContext(), cursor));
    }

    public void setHueCursorTint(ColorStateList tintList) {
        this.mHueCursorTint = tintList;
    }

    public void setHueCursorTint(@ColorInt int tint) {
        setHueCursorTint(ColorStateList.valueOf(tint));
    }
    
    public void setSatValCursor(Drawable cursor) {
        this.mSatValCursor = cursor;
        if (this.mSatValCursor != null) mSatValCursor.setState(getDrawableState());
        invalidate();
    }
    
    public void setSatValCursor(@DrawableRes int cursor) {
        setSatValCursor(AppCompatResources.getDrawable(getContext(), cursor));
    }

    public void setSatValCursorTint(ColorStateList tintList) {
        this.mSatValCursorTint = tintList;
    }

    public void setSatValCursorTint(@ColorInt int tint) {
        setSatValCursorTint(ColorStateList.valueOf(tint));
    }

    public void setOnColorChangedListener(OnColorChangedListener listener) {
        this.mOnColorChangedListener = listener;
    }
    //</editor-fold>

    public interface OnColorChangedListener {
        void onColorChanged(float hue, float saturation, float value, boolean fromUser);
    }
}
