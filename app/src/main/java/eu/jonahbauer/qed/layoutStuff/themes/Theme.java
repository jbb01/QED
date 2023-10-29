package eu.jonahbauer.qed.layoutStuff.themes;

import android.content.Context;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatDelegate;
import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.model.Message;
import eu.jonahbauer.qed.util.Preferences;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Theme {
    LIGHT(R.style.Theme_App, false) {
        @Override
        public @ColorInt int getMessageNameColor(@NonNull Context context, @NonNull Message message) {
            return message.getTransformedColorInt();
        }
    },
    DARK(R.style.Theme_App_Dark, true) {
        @Override
        public @ColorInt int getMessageNameColor(@NonNull Context context, @NonNull Message message) {
            return message.getColorInt();
        }
    },
    ;

    private static Theme currentTheme;
    private static final @AttrRes int[] accentColors = {
            R.attr.accentColor0, R.attr.accentColor1,
            R.attr.accentColor2, R.attr.accentColor3,
            R.attr.accentColor4, R.attr.accentColor5,
            R.attr.accentColor6, R.attr.accentColor7,
            R.attr.accentColor8, R.attr.accentColor9,
    };
    private static final @DrawableRes int[] patterns = {
            R.drawable.background_0, R.drawable.background_1,
            R.drawable.background_2, R.drawable.background_3,
            R.drawable.background_4, R.drawable.background_5,
            R.drawable.background_6, R.drawable.background_7
    };

    private final @StyleRes int theme;
    private final boolean dark;

    public static @NonNull Theme getCurrentTheme() {
        if (currentTheme == null) {
            currentTheme = Preferences.getGeneral().getTheme();
        }
        return currentTheme;
    }

    static void setCurrentTheme(@NonNull Theme theme) {
        currentTheme = theme;
    }

    public void apply(@NonNull Context context) {
        AppCompatDelegate.setDefaultNightMode(this.isDark()
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO
        );
        context.setTheme(getTheme());
    }

    public abstract @ColorInt int getMessageNameColor(@NonNull Context context, @NonNull Message message);

    public @ColorInt int getIconColor(@NonNull Context context, long seed) {
        return Colors.getColor(context, getAccentColorResource(seed));
    }

    public @ColorInt int getSheetBackgroundColor(@NonNull Context context, long seed) {
        return getIconColor(context, seed);
    }

    public @ColorInt int getSheetBackgroundColorDark(@NonNull Context context, long seed) {
        return Colors.multiply(getSheetBackgroundColor(context, seed), 0xFFCCCCCC);
    }

    public @ColorInt int getSheetBackgroundColor(@NonNull Context context, @NonNull Message message) {
        return getMessageNameColor(context, message);
    }

    public @ColorInt int getSheetBackgroundColorDark(@NonNull Context context, @NonNull Message message) {
        return Colors.multiply(getSheetBackgroundColor(context, message), 0xFFCCCCCC);
    }

    public @DrawableRes int getSheetBackgroundPattern(@NonNull Context context, long seed) {
        final int max = patterns.length;
        return patterns[(int) ((seed % max + max) % max)];
    }

    @AttrRes
    private static int getAccentColorResource(long seed) {
        final int max = accentColors.length;
        return accentColors[(int) ((seed % max + max) % max)];
    }
}
