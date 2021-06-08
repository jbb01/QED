package com.jonahbauer.qed.util;

import android.graphics.Color;

import androidx.annotation.ColorInt;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Colors {
    @ColorInt
    public static int multiply(@ColorInt int color1, @ColorInt int color2) {
        return Color.argb(
                Color.alpha(color1) * Color.alpha(color2) / 255,
                Color.red(color1) * Color.red(color2) / 255,
                Color.green(color1) * Color.green(color2) / 255,
                Color.blue(color1) * Color.blue(color2) / 255
        );
    }
}
