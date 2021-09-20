package com.jonahbauer.qed.layoutStuff.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import com.jonahbauer.qed.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class Histogram extends AppCompatImageView {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = true, value = {HISTOGRAM_MODE_BLUE, HISTOGRAM_MODE_GREEN, HISTOGRAM_MODE_RED, HISTOGRAM_MODE_LUMINOSITY})
    public @interface HistogramMode {}

    private static final int HISTOGRAM_MODE_RED = 1;
    private static final int HISTOGRAM_MODE_GREEN = 1 << 1;
    private static final int HISTOGRAM_MODE_BLUE = 1 << 2;
    private static final int HISTOGRAM_MODE_LUMINOSITY = 1 << 3;

    @HistogramMode
    private int mMode = HISTOGRAM_MODE_RED | HISTOGRAM_MODE_GREEN | HISTOGRAM_MODE_BLUE;

    private Bitmap mBitmap;

    public Histogram(Context context) {
        super(context);
        setup(context, null, 0, 0);
    }

    public Histogram(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context, attrs, 0, 0);
    }

    public Histogram(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup(context, attrs, defStyleAttr, 0);
    }

    @Override
    public void invalidate() {
        super.invalidate();
    }

    public void setBitmap(Bitmap bitmap) {
        if (this.mBitmap != bitmap && !(this.mBitmap != null && this.mBitmap.sameAs(bitmap))) {
            this.mBitmap = bitmap;
            setImageDrawable(null);
            if (this.mBitmap != null) {
                HistogramData data = new HistogramData();
                var task = Observable.fromRunnable(() -> {
                    int width = bitmap.getWidth();
                    int height = bitmap.getHeight();

                    int[] pixels = new int[width * height];
                    bitmap.getPixels(pixels, 0, width, 0, 0, width, height);


                    for (int y = 0; y < height; y++) {
                        int offset = y * width;
                        for (int x = 0; x < width; x++) {
                            int pixel = pixels[offset + x];
                            data.add(Color.red(pixel), Color.green(pixel), Color.blue(pixel));
                        }
                    }
                });

                task.subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnComplete(() -> {
                        HistogramDrawable drawable = new HistogramDrawable(data, mMode);
                        this.setImageDrawable(drawable);
                    })
                    .delay(20, TimeUnit.MILLISECONDS)
                    .doOnComplete(() -> {
                        this.setVisibility(VISIBLE);
                        Animation animation = AnimationUtils.loadAnimation(this.getContext(), R.anim.scale_up);
                        this.startAnimation(animation);
                    })
                    .subscribe();
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void setup(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.Histogram, defStyleAttr, defStyleRes);

            mMode = typedArray.getInt(R.styleable.Histogram_mode, 7);

            Drawable drawable = typedArray.getDrawable(R.styleable.Histogram_android_bitmap);
            if (drawable instanceof BitmapDrawable) {
                mBitmap = ((BitmapDrawable) drawable).getBitmap();
            } else if (drawable != null) {
                throw new IllegalArgumentException("Only BitmapDrawables are supported.");
            }

            typedArray.recycle();
        }

        setBitmap(mBitmap);
    }

    /**
     * An object used to store histogram data.
     */
    private static class HistogramData {
        final int[] B = new int[256];
        final int[] G = new int[256];
        final int[] R = new int[256];
        final int[] Y = new int[256];
        int totalPixelCount = 0;

        HistogramData() {}

        void add(int r, int g, int b) {
            this.totalPixelCount++;
            R[r]++;
            G[g]++;
            B[b]++;
            Y[(int)(0.2126 * r + 0.7152 * g + 0.0722 * b)]++;
        }
    }

    /**
     * The actual visualization of the histogram.
     */
    private static class HistogramDrawable extends Drawable {
        final HistogramData histogram;
        private float mRatioX;
        private float mRatioY;
        private int mTopY;
        @HistogramMode
        private final int mMode;

        HistogramDrawable(HistogramData histogram, @HistogramMode int mode) {
            this.histogram = histogram;
            if (histogram != null) {
                this.mTopY = (int) (histogram.totalPixelCount * 0.05F);
            }

            this.mMode = mode;
        }

        private void drawHistogram(@NonNull Canvas canvas, int[] data, int color) {
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColor(color);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));

            Path path = new Path();
            path.moveTo(0.0F, canvas.getHeight());

            for (color = 0; color < 256; color++) {
                int count = data[color];
                if (count > this.mTopY) {
                    count = this.mTopY;
                }

                float pointX = color * this.mRatioX;
                float pointY = count * this.mRatioY;

                path.lineTo(pointX, canvas.getHeight() - pointY);
                if (color == 255) {
                    path.lineTo(pointX, canvas.getHeight());
                }
            }

            path.lineTo(0.0F, canvas.getHeight());
            canvas.drawPath(path, paint);
        }

        private static int getMaxValue(@NonNull int[] array) {
            int max = 0;
            for (int value : array) {
                if (value > max) {
                    max = value;
                }
            }

            return max;
        }

        public void draw(@NonNull Canvas canvas) {
            Rect bound = getBounds();
            float max = 0;

            if ((mMode & HISTOGRAM_MODE_RED) != 0) {
                int maxR = getMaxValue(histogram.R);
                if (maxR > max) max = maxR;
            }
            if ((mMode & HISTOGRAM_MODE_GREEN) != 0) {
                int maxG = getMaxValue(histogram.G);
                if (maxG > max) max = maxG;
            }
            if ((mMode & HISTOGRAM_MODE_BLUE) != 0) {
                int maxB = getMaxValue(histogram.B);
                if (maxB > max) max = maxB;
            }
            if ((mMode & HISTOGRAM_MODE_LUMINOSITY) != 0) {
                int maxY = getMaxValue(histogram.Y);
                if (maxY > max) max = maxY;
            }


            if (max > mTopY) {
                max = mTopY;
            }


            this.mRatioX = (float) bound.width() / 255.0F;
            if (max != 0.0F) {
                max = (float) bound.height() / max;
            } else {
                max = 1.0F;
            }

            this.mRatioY = max;

            if ((mMode & HISTOGRAM_MODE_RED) != 0) {
                this.drawHistogram(canvas, this.histogram.R, Color.RED);
            }
            if ((mMode & HISTOGRAM_MODE_GREEN) != 0) {
                this.drawHistogram(canvas, this.histogram.G, Color.GREEN);
            }
            if ((mMode & HISTOGRAM_MODE_BLUE) != 0) {
                this.drawHistogram(canvas, this.histogram.B, Color.BLUE);
            }
            if ((mMode & HISTOGRAM_MODE_LUMINOSITY) != 0) {
                this.drawHistogram(canvas, this.histogram.Y, Color.WHITE);
            }
        }

        public int getOpacity() {
            return PixelFormat.UNKNOWN;
        }

        public void setAlpha(int alpha) {}

        public void setColorFilter(ColorFilter colorFilter) {}
    }
}
