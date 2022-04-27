package com.jonahbauer.qed.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.ColorSpace;
import android.os.Build;
import android.util.TypedValue;
import androidx.annotation.*;
import androidx.core.math.MathUtils;

import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@UtilityClass
public class Colors {

    /**
     * Multiplies two colors component-wise.
     */
    public static @ColorInt int multiply(@ColorInt int color1, @ColorInt int color2) {
        return Color.argb(
                Color.alpha(color1) * Color.alpha(color2) / 255,
                Color.red(color1) * Color.red(color2) / 255,
                Color.green(color1) * Color.green(color2) / 255,
                Color.blue(color1) * Color.blue(color2) / 255
        );
    }

    /**
     * Resolves {@link android.R.attr#colorPrimary} in the given context.
     */
    public static @ColorInt int getPrimaryColor(@NonNull Context context) {
        return getColor(context, android.R.attr.colorPrimary);
    }

    /**
     * Resolves {@link android.R.attr#colorBackground} in the given context.
     */
    public static @ColorInt int getBackgroundColor(@NonNull Context context) {
        return getColor(context, android.R.attr.colorBackground);
    }

    /**
     * Resolves a color attribute in the theme of the given context.
     * @throws IllegalArgumentException when the given attribute does not resolve to a color.
     */
    public static @ColorInt int getColor(@NonNull Context context, @AttrRes int color) {
        return getColor(context.getTheme(), color);
    }

    /**
     * Resolves a color attribute in the given theme.
     * @throws IllegalArgumentException when the given attribute does not resolve to a color.
     */
    public static @ColorInt int getColor(@NonNull Resources.Theme theme, @AttrRes int color) {
        TypedValue value = new TypedValue();
        theme.resolveAttribute(color, value, true);
        if (value.type < TypedValue.TYPE_FIRST_COLOR_INT || value.type > TypedValue.TYPE_LAST_COLOR_INT) {
            throw new IllegalArgumentException("Cannot resolve " + theme.getResources().getResourceName(color) + " as a color.");
        }
        return value.data;
    }

    /**
     * Calculates the chat color for a given name.
     */
    public static @ColorInt int getColorForName(String name) {
        var bytes = name.getBytes(StandardCharsets.UTF_8);
        return getColorForName(bytes, 0, bytes.length);
    }

    /**
     * Calculates the chat color for a given name represented as UTF-8 bytes in an array.
     */
    public static @ColorInt int getColorForName(byte[] bytes, int offset, int length) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");

            md5.update((byte) 'a');
            md5.update(bytes, offset, length);
            md5.update((byte) 'a');
            var red = getComponentFromBytes(md5.digest());

            md5.update((byte) 'b');
            md5.update(bytes, offset, length);
            md5.update((byte) 'b');
            var green = getComponentFromBytes(md5.digest());

            md5.update((byte) 'c');
            md5.update(bytes, offset, length);
            md5.update((byte) 'c');
            var blue = getComponentFromBytes(md5.digest());

            return Color.rgb(red, green, blue);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Converts the last 7 nibbles of the given byte array to an integer.
     */
    private static int getComponentFromBytes(byte[] bytes) {
        var out = 0;
        out |= ((int) bytes[bytes.length - 1]) & 0xFF;
        out |= (((int) bytes[bytes.length - 2]) & 0xFF) << 8;
        out |= (((int) bytes[bytes.length - 3]) & 0xFF) << 16;
        out |= (((int) bytes[bytes.length - 4]) & 0xF) << 24;
        return out % 156 + 100;
    }

    /**
     * Calculates the squared distance of two argb colors.
     */
    public static int distance(@ColorInt int a, @ColorInt int b) {
        var alpha = Color.alpha(a) - Color.alpha(b);
        var red = Color.red(a) - Color.red(b);
        var green = Color.green(a) - Color.green(b);
        var blue = Color.blue(a) - Color.blue(b);
        return alpha * alpha + red * red + green * green + blue * blue;
    }

    /**
     * Calculates the ΔE of two colors, i.e. the euclidean distance of the color vectors in the
     * {@linkplain ColorSpace.Named#CIE_LAB CIELAB} color space.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static double deltaE(@ColorInt int a, @ColorInt int b) {
        var labA = Color.valueOf(a).convert(ColorSpace.get(ColorSpace.Named.CIE_LAB));
        var labB = Color.valueOf(b).convert(ColorSpace.get(ColorSpace.Named.CIE_LAB));

        var sum = 0d;
        for (int i = 0; i < 3; i++) {
            var d = labA.getComponent(i) - labB.getComponent(i);
            sum += d * d;
        }
        return Math.sqrt(sum);
    }

    /**
     * Tries to find a name with the specified color by prepending the given name with spaces and tabs.
     */
    @WorkerThread
    @Nullable
    public static String findColor(@NonNull String name, @ColorInt int color, int start, int count) throws InterruptedException {
        final int PREFIX_LENGTH = 32;

        var nameBytes = name.getBytes(StandardCharsets.UTF_8);

        var bytes = new byte[PREFIX_LENGTH + nameBytes.length];
        System.arraycopy(nameBytes, 0, bytes, PREFIX_LENGTH, nameBytes.length);
        int offset = PREFIX_LENGTH;
        int length = bytes.length;

        String bestName = null;
        int bestDistance = Integer.MAX_VALUE;

        offset -= skipUpdate(bytes, PREFIX_LENGTH - 1, start);
        for (int i = start; i < count; i++) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            var tmpColor = getColorForName(bytes, offset, length - offset);
            var tmpDistance = distance(tmpColor, color);

            if (tmpDistance < bestDistance) {
                bestName = new String(bytes, offset, length - offset, StandardCharsets.UTF_8);
                bestDistance = tmpDistance;

                if (tmpDistance == 0) break;
            }

            offset -= update(bytes, PREFIX_LENGTH - 1);
            if (offset < 0) break;
        }

        return bestName;
    }

    private static int update(byte[] bytes, int index) {
        if (bytes[index] == ' ') {
            bytes[index] = '\t';
            return 0;
        } else if (bytes[index] == '\t') {
            bytes[index] = ' ';
            return update(bytes, index - 1);
        } else {
            bytes[index] = ' ';
            return 1;
        }
    }

    private static int skipUpdate(byte[] bytes, int index, int target) {
        if (target == 0) return 0;
        target--;

        int out = 0;
        while (target != 0) {
            bytes[index--] = (byte) (target % 2 == 0 ? ' ' : '\t');
            target /= 2;
            out ++;
        }

        return out;
    }

    /**
     * Adjusts the given color for use on a light background by first increasing the saturation and then darkening it
     * to ensure a minimum <a href="https://www.w3.org/TR/WCAG20/#contrast-ratiodef">contrast ratio</a> on white of 2.
     */
    public static @ColorInt int transformColor(@ColorInt int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);

        // sat(x > 155/255) = 1
        // sat(0) = 0
        // sat'(155/255) = 0
        if (hsv[1] > 0.60784316f) {
            hsv[1] = 1;
        } else {
            hsv[1] = -(hsv[1] - 1.2156863f) * hsv[1] * 2.7065556f;
            hsv[1] = MathUtils.clamp(hsv[1], 0, 1);
        }
        color = Color.HSVToColor(hsv);

        // ensure a minimum contrast
        // see https://www.w3.org/TR/WCAG20/#contrast-ratiodef
        // see https://www.w3.org/TR/WCAG20/#relativeluminancedef
        final var minimumContrast = 2;

        // see ColorSpace.Rgb#getEotf()
        double r = Math.pow(Color.red(color) / 255.0, 2.2);
        double g = Math.pow(Color.green(color) / 255.0, 2.2);
        double b = Math.pow(Color.blue(color) / 255.0, 2.2);

        var luminance = (float) ((0.2126 * r) + (0.7152 * g) + (0.0722 * b));
        var contrast = 1.05f / (luminance + 0.05f);
        if (contrast < minimumContrast) {
            var targetLuminance = 1.05 / minimumContrast - 0.05;
            var factor = targetLuminance / luminance;

            // see ColorSpace.Rgb.getOetf()
            color = Color.rgb(
                    (int) (Math.pow(r * factor, 1 / 2.2) * 255),
                    (int) (Math.pow(g * factor, 1 / 2.2) * 255),
                    (int) (Math.pow(b * factor, 1 / 2.2) * 255)
            );
        }

        return color;
    }
}
