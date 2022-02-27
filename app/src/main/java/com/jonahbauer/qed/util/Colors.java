package com.jonahbauer.qed.util;

import android.content.res.Resources;
import android.graphics.Color;
import android.util.TypedValue;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Colors {

    public static @ColorInt int multiply(@ColorInt int color1, @ColorInt int color2) {
        return Color.argb(
                Color.alpha(color1) * Color.alpha(color2) / 255,
                Color.red(color1) * Color.red(color2) / 255,
                Color.green(color1) * Color.green(color2) / 255,
                Color.blue(color1) * Color.blue(color2) / 255
        );
    }

    public static @ColorInt int getColor(@NonNull Resources.Theme theme, @AttrRes int color) {
        TypedValue value = new TypedValue();
        theme.resolveAttribute(color, value, true);
        return value.data;
    }
}
