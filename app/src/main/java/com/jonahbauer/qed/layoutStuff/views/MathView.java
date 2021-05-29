/*
 *  Based on https://github.com/jianzhongli/MathView
 */
package com.jonahbauer.qed.layoutStuff.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.ViewParent;
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

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.util.Triple;
import com.x5.template.Chunk;
import com.x5.template.Theme;
import com.x5.template.providers.AndroidTemplates;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class MathView extends LinearLayout {
    @SuppressWarnings("RegExpRedundantEscape")
    private static final String LATEX = "((?<=\\\\\\])|(?=\\\\\\[)|(?<=\\\\\\))|(?=\\\\\\())";

    private final Context mContext;

    private final List<TextView> mTextViews = new LinkedList<>();
    private final List<Triple<String, Integer, Long>> mInternalMathViews = new LinkedList<>();

    private String mText;
    private int mTextStyle;
    @Dimension @Px private float mTextSize;
    @ColorInt private int mTextColor;
    @StyleRes private int mTextAppearance = -1;

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
        this.mContext = context;
        this.setOrientation(VERTICAL);

        setup(context, attrs, defStyleAttr, defStyleRes);
    }

    private void setup(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        if (attrs == null) return;

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MathView, defStyleAttr, defStyleRes);

        TextView _default = new TextView(context);
        mTextSize = _default.getTextSize();
        mTextStyle = _default.getTypeface().getStyle();
        mTextColor = _default.getCurrentTextColor();

        mText = typedArray.getString(R.styleable.MathView_android_text);
        mTextSize = typedArray.getDimension(R.styleable.MathView_android_textSize, mTextSize);
        mTextStyle = typedArray.getInt(R.styleable.MathView_android_textStyle, mTextStyle);
        mTextColor = typedArray.getColor(R.styleable.MathView_android_textColor, mTextColor);
        mTextAppearance = typedArray.getResourceId(R.styleable.MathView_android_textAppearance, -1);

        if (mTextAppearance != -1) _default.setTextAppearance(mTextAppearance);
        _default.setTextSize(mTextSize);
        _default.setTypeface(_default.getTypeface(), mTextStyle);
        _default.setTextColor(mTextColor);

        typedArray.recycle();

        setText(mText);
    }

    public void setText(@Nullable String text) {
        this.setText(text, -1);
    }
    public void setText(@Nullable String text, long id) {
        if (Objects.equals(text, mText)) return;

        release();

        this.removeAllViews();
        mTextViews.clear();
        mInternalMathViews.clear();
        mText = text;
        if (text == null) return;

        String[] laTeX = getLaTeX(text);

        for (int i = 0; i < laTeX.length; i++) {
            String str = laTeX[i];
            boolean inline = str.startsWith("\\(") && str.endsWith("\\)");
            boolean displayed = str.startsWith("\\[") && str.endsWith("\\]");
            if (displayed || inline) {
                // add as math view
                str = "\\[" + str.substring(2, str.length() - 2) + "\\]";
                InternalMathView mathView = InternalMathView.getInternalMathView(mContext, null, str, inline, mTextSize, i, id,this);

                mInternalMathViews.add(new Triple<>(str, i, id));

                this.addView(mathView);
            } else {
                // add as text view
                TextView textView = new TextView(mContext);

                textView.setText(str);
                mTextViews.add(textView);

                if (mTextAppearance != -1) textView.setTextAppearance(mTextAppearance);
                textView.setTypeface(textView.getTypeface(), mTextStyle);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextSize);
                textView.setTextColor(mTextColor);

                this.addView(textView);
            }
        }
    }

    @SuppressWarnings("unused")
    public void setTextStyle(int textStyle) {
        mTextViews.forEach(t -> t.setTypeface(t.getTypeface(), textStyle));
    }

    public void setTextSize(@Dimension(unit = Dimension.SP) float size) {
        this.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
    }
    public void setTextSize(int unit, float size) {
        mTextViews.forEach(t -> t.setTextSize(unit, size));
    }

    public void setTextColor(@ColorInt int color) {
        mTextViews.forEach(t -> t.setTextColor(color));
    }

    public void setTextAppearance(@StyleRes int resId) {
        mTextViews.forEach(t -> t.setTextAppearance(resId));
    }

    @NonNull
    private static String[] getLaTeX(@NonNull String str) {
        String[] split = str.split(LATEX);

        ArrayList<String> out = new ArrayList<>();
        int depth = 0;
        StringBuilder tmp = new StringBuilder();
        for (String s : split) {
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
     * Release all {@code InternalMathView}s associated with this view
     * so that they can be claimed by other {@code MathView}s.
     */
    public void release() {
        for (Triple<String, Integer, Long> internalMathView : mInternalMathViews) {
            InternalMathView.release(internalMathView.first, internalMathView.second, internalMathView.third, this);
        }
    }

    /**
     * Clear {@code InternalMathView} cache.
     */
    public static void clearCache() {
        InternalMathView.cache.clear();
    }

    /**
     * Extract all latex components of the given text, create {@code InternalMathView}s for them and preload them.
     * @param context a context for view creation
     * @param text a text possibly containing latex
     * @param size font size
     * @param id message id (used for caching)
     * @param preloadLayout a layout for the view to preload
     */
    public static void extractAndPreload(Context context, String text, float size, long id, @Nullable LinearLayout preloadLayout) {
        String[] split = getLaTeX(text);
        for (int i = 0; i < split.length; i++) {
            boolean inline = split[i].startsWith("\\(") && split[i].endsWith("\\)");
            boolean displayed = split[i].startsWith("\\[") && split[i].endsWith("\\]");

            if (inline) InternalMathView.preload(context, "\\[" + split[i].substring(2, split[i].length() - 2) + "\\]", true, size, preloadLayout, i, id);
            else if (displayed) InternalMathView.preload(context, split[i], false, size, preloadLayout, i, id);
        }
    }

    private static class InternalMathView extends WebView {
        /**
         * Owned InternalMathView:      Objects.hash(laTeX, index, id, owner)
         * Unowned InternalMathView:    Objects.hash(laTeX, index, id)
         *
         * laTeX: displayed LaTeX literal
         * index: part index of complete message
         * id:    message id
         * owner: displaying MathView
         */
        private static final SparseArray<SoftReference<InternalMathView>> cache = new SparseArray<>();

        /**
         * @param context a context for view creation
         * @param attrs a attribute set used for creating a new InternalMathView
         * @param laTeX the latex literal to be displayed
         * @param inline inline or displayed latex
         * @param size font size
         * @param index part index as part of whole message (used for caching)
         * @param id message id (used for caching)
         * @param owner displaying MathView
         */
        public static InternalMathView getInternalMathView(Context context, AttributeSet attrs, String laTeX, boolean inline, float size, int index, long id, MathView owner) {
            int cachePos = Objects.hash(laTeX, index, id, owner);

            // load owned math view
            SoftReference<InternalMathView> ref = cache.get(cachePos);
            if (ref != null) {
                InternalMathView mathView = ref.get();
                if (mathView != null) {
                    ViewParent parent = mathView.getParent();
                    if (parent != null) ((ViewGroup)parent).removeView(mathView);

                    return mathView;
                }
            }

            // load unowned math view
            int preloadedCachePos = Objects.hash(laTeX, index, id);
            SoftReference<InternalMathView> preloadedRef = cache.get(preloadedCachePos);
            if (preloadedRef != null) {
                InternalMathView mathView = preloadedRef.get();
                if (mathView != null) {
                    ViewParent parent = mathView.getParent();
                    if (parent != null) ((ViewGroup)parent).removeView(mathView);

                    // take ownership
                    cache.remove(preloadedCachePos);
                    cache.put(cachePos, preloadedRef);

                    return mathView;
                }
            }

            InternalMathView internalMathView = new InternalMathView(context, attrs, laTeX, inline, size);
            cache.put(cachePos, new SoftReference<>(internalMathView));
            return internalMathView;
        }

        /**
         * @param context a context for view creation
         * @param laTeX a latex literal to be displayed
         * @param inline inline or displayed latex
         * @param size font size
         * @param preloadLayout a layout for the view to preload
         * @param index part index as part of whole message (used for caching)
         * @param id message id (used for caching)
         */
        public static void preload(Context context, String laTeX, boolean inline, float size, @Nullable LinearLayout preloadLayout, int index, long id) {
            InternalMathView mathView = new InternalMathView(context, null, laTeX, inline, size);
            if (preloadLayout != null) {
                preloadLayout.addView(mathView);
            }

            cache.put(Objects.hash(laTeX, index, id), new SoftReference<>(mathView));
        }

        /**
         * Give up ownership for a {@code InternalMathView} so that it can be claimed by other {@code MathView}s.
         * @param laTeX a latex literal to be displayed
         * @param index part index as part of whole message (used for caching)
         * @param id message id (used for caching)
         * @param owner the current owner of the {@code InternalMathView}
         */
        public static void release(String laTeX, int index, long id, MathView owner) {
            int ownedPos = Objects.hash(laTeX, index, id, owner);

            SoftReference<InternalMathView> ref = cache.get(ownedPos);
            if (ref != null && ref.get() != null) {
                int unownedPos = Objects.hash(laTeX, index, id);
                cache.remove(ownedPos);
                cache.put(unownedPos, ref);
            }
        }


        private String laTeX;
        private final boolean inline;
        private float startX;
        private int scrollStartX;
        private int maxScrollX;


        @SuppressLint("SetJavaScriptEnabled")
        public InternalMathView(Context context, AttributeSet attrs, String laTeX, boolean inline, float size) {
            super(context, attrs);

            this.inline = inline;

            if (inline) setScrollBarSize(0);

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (inline) {
                layoutParams.setMargins(0, - (int) size, 0, - (int) size);
            }
            setLayoutParams(layoutParams);

            getSettings().setJavaScriptEnabled(true);
            getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            getSettings().setDefaultFontSize((int)(size * 0.5f));
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

            Chunk chunk = getChunk();
            chunk.set("formula", laTeX);

            this.loadDataWithBaseURL(null, chunk.toString(), "text/html", "utf-8", "about:blank");
        }

        public String getText() {
            return laTeX;
        }
    }
}
