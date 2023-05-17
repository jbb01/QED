package eu.jonahbauer.qed.util;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import eu.jonahbauer.qed.R;
import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

@UtilityClass
public class TextUtils {
    /**
     * Returns {@code true} when the string is {@code null} or {@linkplain #isBlank(String) blank}.
     */
    public static boolean isNullOrBlank(@Nullable String string) {
        return string == null || isBlank(string);
    }

    /**
     * Returns {@code true} when all code points in the string are {@linkplain Character#isWhitespace(int) whitespaces}.
     */
    public static boolean isBlank(@NonNull String string) {
        return string.isEmpty() || string.codePoints().allMatch(Character::isWhitespace);
    }

    @SafeVarargs
    public static <T> String formatRange(@NonNull Context context, Function<T, String> formatter, T...items) {
        return formatRange(context, formatter, Arrays.asList(items));
    }

    public static <T> String formatRange(@NonNull Context context, Function<T, String> formatter, List<T> items) {
        if (items == null || items.isEmpty()) return null;
        return context.getString(R.string.range, formatter.apply(items.get(0)), formatter.apply(items.get(items.size() - 1)));
    }
}
