package com.jonahbauer.qed.util;

import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;

import com.jonahbauer.qed.R;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Themes {
    @ColorInt
    public static int colorful(long seed) {
        switch ((int) ((seed % 10 + 10) % 10)) {
            case 0:
                return Color.argb(255, 0x33, 0xb5, 0xe5);
            case 1:
                return Color.argb(255, 0x99, 0xcc, 0x00);
            case 2:
                return Color.argb(255, 0xff, 0x44, 0x44);
            case 3:
                return Color.argb(255, 0x00, 0x99, 0xcc);
            case 4:
                return Color.argb(255, 0x66, 0x99, 0x00);
            case 5:
                return Color.argb(255, 0xcc, 0x00, 0x00);
            case 6:
                return Color.argb(255, 0xaa, 0x66, 0xcc);
            case 7:
                return Color.argb(255, 0xff, 0xbb, 0x33);
            case 8:
                return Color.argb(255, 0xff, 0x88, 0x00);
            case 9:
                return Color.argb(255, 0x00, 0xdd, 0xff);
            default:
                throw new AssertionError();
        }
    }

    @DrawableRes
    public static int pattern(long seed) {
        switch ((int) (seed % 8 + 8) % 8) {
            case 0:
                return R.drawable.background_0;
            case 1:
                return R.drawable.background_1;
            case 2:
                return R.drawable.background_2;
            case 3:
                return R.drawable.background_3;
            case 4:
                return R.drawable.background_4;
            case 5:
                return R.drawable.background_5;
            case 6:
                return R.drawable.background_6;
            case 7:
                return R.drawable.background_7;
            default:
                throw new AssertionError();
        }
    }
}
