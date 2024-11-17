/*
 *  Based on https://github.com/jianzhongli/MathView
 */
package eu.jonahbauer.qed.ui.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.StyleRes;

import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.util.ViewUtils;
import com.x5.template.Chunk;
import com.x5.template.Theme;
import com.x5.template.providers.AndroidTemplates;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class MathView extends LinearLayout {
    @SuppressWarnings("RegExpRedundantEscape")
    private static final String LATEX = "((?<=\\\\\\])|(?=\\\\\\[)|(?<=\\\\\\))|(?=\\\\\\())";

    private final List<TextView> mTextViews = new ArrayList<>();
    private final List<InternalMathView> mInternalMathViews = new ArrayList<>();
    private WeakReference<LinearLayout> mMathPreload;

    private String mText;
    private int mTextStyle;
    @Dimension @Px private float mTextSize;
    @ColorInt private int mTextColor;

    public MathView(Context context) {
        this(context, null);
    }

    public MathView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MathView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public MathView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.setOrientation(VERTICAL);

        setup(context, attrs, defStyleAttr, defStyleRes);
    }

    private void setup(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        if (attrs == null) return;

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MathView, defStyleAttr, defStyleRes);

        mTextSize = ViewUtils.spToPx(this, 14);
        mTextColor = Color.BLACK;

        int textAppearanceResId = typedArray.getResourceId(R.styleable.MathView_android_textAppearance, 0);
        if (textAppearanceResId != 0) {
            loadTextAppearance(textAppearanceResId);
        }

        mTextSize = typedArray.getDimension(R.styleable.MathView_android_textSize, mTextSize);
        mTextColor = typedArray.getColor(R.styleable.MathView_android_textColor, mTextColor);
        mTextStyle = typedArray.getInt(R.styleable.MathView_android_textStyle, mTextStyle);
        mText = typedArray.getString(R.styleable.MathView_android_text);

        typedArray.recycle();

        setText(mText);
    }

    public void setMathPreload(LinearLayout linearLayout) {
        mMathPreload = new WeakReference<>(linearLayout);
    }

    public void setText(@Nullable String text) {
        if (Objects.equals(text, mText)) return;

        release();

        this.removeAllViews();
        mTextViews.clear();
        mInternalMathViews.clear();
        mText = text;
        if (text == null) return;

        String[] latex = getLaTeX(text);

        int position = 0;
        for (String part : latex) {
            boolean inline = part.startsWith("\\(") && part.endsWith("\\)");
            boolean displayed = part.startsWith("\\[") && part.endsWith("\\]");

            if (displayed || inline) {
                // add as math view
                part = "\\[" + part.substring(2, part.length() - 2) + "\\]";

                var mathView = InternalMathView.getInternalMathView(
                        getContext(), text, part, position++, inline
                );
                mathView.setTextSize((int) mTextSize);
                mathView.setTextColor(mTextColor);
                mathView.load();

                mInternalMathViews.add(mathView);

                this.addView(mathView);
            } else {
                // add as text view
                TextView textView = new TextView(getContext());

                textView.setText(part);
                mTextViews.add(textView);

                textView.setTypeface(textView.getTypeface(), mTextStyle);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextSize);
                textView.setTextColor(mTextColor);

                this.addView(textView);
            }
        }
    }

    //<editor-fold desc="Style" defaulstate="collapsed">
    private void loadTextAppearance(@StyleRes int textAppearance) {
        TypedArray array = getContext().obtainStyledAttributes(
                textAppearance,
                new int[] {android.R.attr.textSize, android.R.attr.textColor, android.R.attr.textStyle}
        );
        mTextSize = array.getDimension(0, mTextSize);
        mTextColor = array.getColor(1, mTextColor);
        mTextStyle = array.getInt(2, 0);

        array.recycle();
    }

    private void updateTextStyle() {
        mTextViews.forEach(t -> t.setTypeface(t.getTypeface(), mTextStyle));
    }

    private void updateTextSize() {
        mTextViews.forEach(t -> t.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextSize));
        mInternalMathViews.forEach(t -> t.setTextSize((int) mTextSize));
    }

    private void updateTextColor() {
        mTextViews.forEach(t -> t.setTextColor(mTextColor));
        mInternalMathViews.forEach(t -> t.setTextColor(mTextColor));
    }

    @SuppressWarnings("unused")
    public void setTextStyle(int textStyle) {
        mTextStyle = textStyle;
        updateTextStyle();
    }

    public void setTextSize(@Dimension(unit = Dimension.SP) float size) {
        this.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
    }

    public void setTextSize(int unit, float size) {
        mTextSize = TypedValue.applyDimension(unit, size, getResources().getDisplayMetrics());
        updateTextSize();
    }

    public void setTextColor(@ColorInt int color) {
        mTextColor = color;
        updateTextColor();
    }

    public void setTextAppearance(@StyleRes int resId) {
        loadTextAppearance(resId);

        updateTextSize();
        updateTextSize();
        updateTextStyle();
    }
    //</editor-fold>

    /**
     * Release all {@code InternalMathView}s associated with this view
     * so that they can be claimed by other {@code MathView}s.
     */
    public void release() {
        var preloadLayout = mMathPreload != null ? mMathPreload.get() : null;
        for (int i = 0; i < mInternalMathViews.size(); i++) {
            var mathView = mInternalMathViews.get(i);
            InternalMathView.release(mText, mathView.getText(), i, mathView, preloadLayout);
        }
        mInternalMathViews.clear();
        mText = null;
    }

    @NonNull
    private static String[] getLaTeX(@NonNull String message) {
        String[] parts = message.split(LATEX);

        ArrayList<String> out = new ArrayList<>();
        int depth = 0;
        StringBuilder tmp = new StringBuilder();
        for (String s : parts) {
            boolean start = s.startsWith("\\["), startInline = s.startsWith("\\(");
            boolean end = s.endsWith("\\]"), endInline = s.endsWith("\\)");
            if (!(start || end || startInline || endInline)) out.add(s);

            if ((start && end) || (startInline && endInline)) {
                tmp.append(s);
                if (depth == 0) {
                    out.add(tmp.toString());
                    tmp.setLength(0);
                }
            } else if (start || startInline) {
                depth++;
                tmp.append(s);
            } else if (end || endInline) {
                depth--;
                tmp.append(s);
                if (depth == 0) {
                    out.add(tmp.toString());
                    tmp.setLength(0);
                }
            }
        }

        if (depth != 0) out.add(tmp.toString());

        return out.stream().filter(s -> s != null && !s.matches("(\\s|\\n|\\r)*")).map(String::trim).toArray(String[]::new);
    }

    /**
     * Clear {@code InternalMathView} cache.
     */
    public static void clearCache() {
        InternalMathView.CACHE.clear();
    }

    public static void extractAndPreload(
            @NonNull Context context, @NonNull String message,
            @Dimension @Px int textSize, @ColorInt int textColor,
            @Nullable LinearLayout preloadLayout
    ) {
        var parts = getLaTeX(message);
        int position = 0;

        for (String part : parts) {
            boolean inline = part.startsWith("\\(") && part.endsWith("\\)");
            boolean displayed = part.startsWith("\\[") && part.endsWith("\\]");

            if (displayed || inline) {
                part = "\\[" + part.substring(2, part.length() - 2) + "\\]";

                InternalMathView.preload(context, message, part, position++, textSize, textColor, inline, preloadLayout);
            }
        }
    }

    private static class InternalMathView extends WebView {
        private static final HashMap<CacheKey, SoftReference<InternalMathView>> CACHE = new HashMap<>();

        public static synchronized InternalMathView getInternalMathView(Context context, String message, String part, int position, boolean inline) {
            var cacheKey = new CacheKey(message, part, position);

            var ref = CACHE.remove(cacheKey);
            if (ref != null) {
                var mathView = ref.get();
                if (mathView != null) {
                    var parent = mathView.getParent();
                    if (parent instanceof ViewGroup) ((ViewGroup) parent).removeView(mathView);
                    mathView.setInline(inline);
                    return mathView;
                }
            }

            InternalMathView internalMathView = new InternalMathView(context, part, inline);
            internalMathView.load();
            return internalMathView;
        }

        public static synchronized void preload(Context context, String message, String part, int position,
                                   @Dimension @Px int textSize, @ColorInt int textColor, boolean inline,
                                   @Nullable LinearLayout preloadLayout) {
            InternalMathView mathView = new InternalMathView(context, part, inline);
            mathView.setTextSize(textSize);
            mathView.setTextColor(textColor);
            mathView.load();

            if (preloadLayout != null) {
                preloadLayout.addView(mathView);
            }

            CACHE.put(new CacheKey(message, part, position), new SoftReference<>(mathView));
        }

        public static synchronized void release(String message, String part, int position, InternalMathView mathView, @Nullable LinearLayout preloadLayout) {
            var parent = mathView.getParent();
            if (parent instanceof ViewGroup) ((ViewGroup) parent).removeView(mathView);

            var previous = CACHE.putIfAbsent(new CacheKey(message, part, position), new SoftReference<>(mathView));
            if (previous == null && preloadLayout != null) {
                preloadLayout.addView(mathView);
            }
        }

        private String laTeX;
        private float startX;
        private int scrollStartX;
        private int maxScrollX;

        private boolean inline;
        @ColorInt
        private int textColor = Color.BLACK;
        @Dimension @Px
        private int textSize = 20;

        private boolean dirty = true;

        @SuppressLint("SetJavaScriptEnabled")
        public InternalMathView(Context context, String laTeX, boolean inline) {
            super(context);

            this.inline = inline;

            if (inline) setScrollBarSize(0);

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            setLayoutParams(layoutParams);

            getSettings().setJavaScriptEnabled(true);
            getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            setBackgroundColor(Color.TRANSPARENT);

            setVerticalScrollBarEnabled(false);
            setScrollContainer(false);

            setText(laTeX);
        }

        @Override
        @SuppressLint("ClickableViewAccessibility")
        public boolean onTouchEvent(MotionEvent event) {
            if (inline) return false;
            int action = event.getAction();

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    startX = event.getX();
                    scrollStartX = getScrollX();
                    maxScrollX = computeHorizontalScrollRange() - getMeasuredWidth();

                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = event.getX() - startX;
                    int scrollX = scrollStartX - (int) dx;
                    if (scrollX < 0) scrollX = 0;
                    else if (scrollX > maxScrollX) scrollX = maxScrollX;

                    setScrollX(scrollX);
                    return true;
                case MotionEvent.ACTION_UP:
                    setTouchDelegate(null);
                    return true;
            }

            return false;
        }

        private Chunk getChunk() {
            AndroidTemplates loader = new AndroidTemplates(getContext());
            return new Theme(loader).makeChunk("katex");
        }

        public void setText(String laTeX) {
            this.laTeX = laTeX;
        }

        public String getText() {
            return laTeX;
        }

        public void setTextColor(@ColorInt int textColor) {
            if (this.textColor != textColor) {
                this.dirty = true;
                this.textColor = textColor;
            }
        }

        public void setTextSize(@Dimension @Px int size) {
            if (this.textSize != size) {
                this.dirty = true;
                this.textSize = size;
                getSettings().setDefaultFontSize(size / 2);
            }
        }

        public void setInline(boolean inline) {
            if (this.inline != inline) {
                this.dirty = true;
                this.inline = inline;
//                setHorizontalScrollBarEnabled(!inline);
            }
        }

        public void load() {
            if (!dirty) return;
            dirty = false;

            if (inline) {
                ((LinearLayout.LayoutParams) getLayoutParams()).setMargins(0, -textSize, 0, -textSize);
            }

            Chunk chunk = getChunk();
            chunk.set("formula", laTeX);

            String hexColor = String.format("#%06X", (0xFFFFFF & textColor));
            chunk.set("textColor", hexColor);

            this.loadDataWithBaseURL(null, chunk.toString(), "text/html", "utf-8", "about:blank");
        }

        private static class CacheKey {
            public String message;
            public String part;
            public int position;

            public CacheKey(String message, String part, int position) {
                this.message = message;
                this.part = part;
                this.position = position;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                CacheKey cacheKey = (CacheKey) o;
                return position == cacheKey.position
                       && Objects.equals(message, cacheKey.message)
                       && Objects.equals(part, cacheKey.part);
            }

            @Override
            public int hashCode() {
                return Objects.hash(message, part, position);
            }
        }
    }
}
