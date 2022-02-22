package com.jonahbauer.qed.util;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import com.jonahbauer.qed.R;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Themes {
    private static final @AttrRes int[] ACCENT_COLORS = {
            R.attr.accentColor0, R.attr.accentColor1,
            R.attr.accentColor2, R.attr.accentColor3,
            R.attr.accentColor4, R.attr.accentColor5,
            R.attr.accentColor6, R.attr.accentColor7,
            R.attr.accentColor8, R.attr.accentColor9,
    };
    private static final @DrawableRes int[] PATTERNS = {
            R.drawable.background_0, R.drawable.background_1,
            R.drawable.background_2, R.drawable.background_3,
            R.drawable.background_4, R.drawable.background_5,
            R.drawable.background_6, R.drawable.background_7
    };

    @ColorInt
    public static int colorful(Context context, long seed) {
        return colorful(context.getTheme(), seed);
    }

    @ColorInt
    public static int colorful(Resources.Theme theme, long seed) {
        @AttrRes int color = colorful(seed);
        TypedValue value = new TypedValue();
        theme.resolveAttribute(color, value, true);
        return value.data;
    }

    @AttrRes
    private static int colorful(long seed) {
        final int MAX = ACCENT_COLORS.length;
        return ACCENT_COLORS[(int) ((seed % MAX + MAX) % MAX)];
    }

    @DrawableRes
    public static int pattern(long seed) {
        final int MAX = PATTERNS.length;
        return PATTERNS[(int) ((seed % MAX + MAX) % MAX)];
    }
}
