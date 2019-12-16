/*
 *  Based on https://github.com/jianzhongli/MathView
 */
package com.jonahbauer.qed.layoutStuff;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import com.jonahbauer.qed.Application;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.chat.Message;
import com.x5.template.Chunk;
import com.x5.template.Theme;
import com.x5.template.providers.AndroidTemplates;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("unused")
public class MathView extends LinearLayout {
    @SuppressWarnings("RegExpRedundantEscape")
    private static final String LATEX = "((?<=\\\\\\])|(?=\\\\\\[)|(?<=\\\\\\))|(?=\\\\\\())";

    private Context context;

    private List<TextView> textViews = new LinkedList<>();

    private float textSize;
    private int textStyle;
    private int textColor;
    private int textAppearance = -1;

    private float spaceWidth;

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
        this.context = context;
        this.setOrientation(VERTICAL);

        setup(context, attrs, defStyleAttr, defStyleRes);
    }

    private void setup(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        if (attrs == null) return;

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MathView, defStyleAttr, defStyleRes);

        TextView _default = new TextView(context);
        textSize = _default.getTextSize();
        textStyle = _default.getTypeface().getStyle();
        textColor = _default.getCurrentTextColor();

        textSize = typedArray.getDimension(R.styleable.MathView_android_textSize, textSize);
        textStyle = typedArray.getInt(R.styleable.MathView_android_textStyle, textStyle);
        textColor = typedArray.getColor(R.styleable.MathView_android_textColor, textColor);
        textAppearance = typedArray.getResourceId(R.styleable.MathView_android_textAppearance, -1);

        if (textAppearance != -1) _default.setTextAppearance(textAppearance);
        _default.setTextSize(textSize);
        _default.setTypeface(_default.getTypeface(), textStyle);
        _default.setTextColor(textColor);

        spaceWidth = _default.getPaint().measureText(" ");

        typedArray.recycle();
    }

    public void setMessage(@Nullable Message message) {
        this.removeAllViews();
        textViews.clear();
        if (message == null) return;

        String[] laTeX = getLaTeX(message.message);

        for (int i = 0; i < laTeX.length; i++) {
            String str = laTeX[i];
            boolean inline = str.startsWith("\\(") && str.endsWith("\\)");
            boolean displayed = str.startsWith("\\[") && str.endsWith("\\]");
            if (displayed || inline) {
                // add as math view
                InternalMathView mathView = InternalMathView.getInternalMathView(context, null, "\\[" + str.substring(2, str.length() - 2) + "\\]", inline, textSize, message.id, i);

                this.addView(mathView);
            } else {
                // add as text view
                TextView textView = new TextView(context);

                textView.setText(str);
                textViews.add(textView);

                if (textAppearance != -1) textView.setTextAppearance(textAppearance);
                textView.setTypeface(textView.getTypeface(), textStyle);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
                textView.setTextColor(textColor);

                this.addView(textView);
            }
        }
    }

    public void setTextStyle(int textStyle) {
        textViews.forEach(t -> t.setTypeface(t.getTypeface(), textStyle));
    }

    public void setTextSize(float size) {
        this.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
    }
    public void setTextSize(int unit, float size) {
        textViews.forEach(t -> t.setTextSize(unit, size));
    }

    public void setTextColor(@ColorInt int color) {
        textViews.forEach(t -> t.setTextColor(color));
    }

    public void setTextAppearance(@StyleRes int resId) {
        textViews.forEach(t -> t.setTextAppearance(resId));
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

    public static boolean extractAndPreload(Context context, Message message, float textSize, @Nullable LinearLayout preloadLayout) {
        String[] split = getLaTeX(message.message);
        boolean out = false;
        for (int i = 0; i < split.length; i++) {
            boolean inline = split[i].startsWith("\\(") && split[i].endsWith("\\)");
            boolean displayed = split[i].startsWith("\\[") && split[i].endsWith("\\]");

            if (inline) InternalMathView.preload(context, "\\[" + split[i].substring(2, split[i].length() - 2) + "\\]", true, textSize, preloadLayout, message.id, i);
            else if (displayed) InternalMathView.preload(context, split[i], false, textSize, preloadLayout, message.id, i);

            out |= inline || displayed;
        }

        return out;
    }

    public float getSpaceWidth() {
        return spaceWidth;
    }

    private static class InternalMathView extends WebView {
        private static final SparseArray<WeakReference<InternalMathView>> cache = new SparseArray<>();

        private String laTeX;
        private boolean inline;

        public static InternalMathView getInternalMathView(Context context, AttributeSet attrs, String laTeX, boolean inline, float size, int id, int index) {
            WeakReference<InternalMathView> ref = cache.get(laTeX.hashCode() ^ id ^ index);
            if (ref != null) {
                InternalMathView mathView = ref.get();
                if (mathView != null) {
                    ViewParent parent = mathView.getParent();
                    if (parent != null) ((ViewGroup)parent).removeView(mathView);

                    return mathView;
                }
            }

            Log.d(Application.LOG_TAG_DEBUG, "created new mathview: " + laTeX);
            InternalMathView internalMathView = new InternalMathView(context, attrs, laTeX, inline, size);
            cache.put(laTeX.hashCode() ^ id ^ index, new WeakReference<>(internalMathView));
            return internalMathView;
        }

        public static void preload(Context context, String laTeX, boolean inline, float size, @Nullable LinearLayout preloadLayout, int id, int index) {
            InternalMathView mathView = new InternalMathView(context, null, laTeX, inline, size);
            if (preloadLayout != null) {
                preloadLayout.addView(mathView);
            }

            cache.put(laTeX.hashCode() ^ id ^ index, new WeakReference<>(mathView));
        }

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

        private float startX;
        private int scrollStartX;
        private int maxScrollX;

        @Override
        @SuppressLint("ClickableViewAccessibility")
        public boolean onTouchEvent(MotionEvent event) {
            if (inline) return false;
            int action = event.getAction();

            switch (action) {
                case (MotionEvent.ACTION_DOWN):
                    startX = event.getX();
                    scrollStartX = getScrollX();
                    maxScrollX = computeHorizontalScrollRange() - getMeasuredWidth();
                    return true;
                case (MotionEvent.ACTION_MOVE):
                    float dx = event.getX() - startX;
                    int scrollX = scrollStartX - (int) dx;
                    if (scrollX < 0) scrollX = 0;
                    else if (scrollX > maxScrollX) scrollX = maxScrollX;

                    setScrollX(scrollX);
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
