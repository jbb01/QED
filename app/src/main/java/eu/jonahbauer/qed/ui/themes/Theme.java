package eu.jonahbauer.qed.ui.themes;

import android.content.Context;
import android.graphics.Color;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatDelegate;
import eu.jonahbauer.qed.Application;
import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.model.Message;
import eu.jonahbauer.qed.util.Colors;
import eu.jonahbauer.qed.util.Preferences;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
@RequiredArgsConstructor
public enum Theme {
    LIGHT(R.style.Theme_App, false) {
        @Override
        public @ColorInt int getMessageNameColor(@NonNull Context context, @NonNull Message message) {
            return message.getTransformedColorInt();
        }

        @Override
        public @ColorInt int getMessageColorForName(@NonNull String name) {
            return Colors.transformColor(Colors.getColorForName(name));
        }
    },
    DARK(R.style.Theme_App_Dark, true) {
        @Override
        public @ColorInt int getMessageNameColor(@NonNull Context context, @NonNull Message message) {
            return message.getColorInt();
        }

        @Override
        public @ColorInt int getMessageColorForName(@NonNull @NotNull String name) {
            return Colors.getColorForName(name);
        }
    },
    WORKERS_RED(R.style.Theme_App_WorkersRed, false) {
        @Override
        public @ColorInt int getMessageNameColor(@NonNull Context context, @NonNull Message message) {
            return context.getColor(R.color.workers_yellow);
        }

        @Override
        public @ColorInt int getMessageColorForName(@NonNull String name) {
            return Color.YELLOW;
        }

        @Override
        public @ColorInt int getSheetBackgroundColor(@NonNull Context context, long seed) {
            return context.getColor(R.color.workers_red);
        }

        @Override
        public @ColorInt int getSheetBackgroundColor(@NonNull Context context, @NonNull Message message) {
            return context.getColor(R.color.workers_red);
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

    public void apply(@NonNull Application context) {
        var oldNightMode = AppCompatDelegate.getDefaultNightMode();
        var newNightMode = this.isDark()
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO;

        if (oldNightMode != newNightMode) {
            AppCompatDelegate.setDefaultNightMode(newNightMode);
        } else {
            context.getActivity().recreate();
        }

        context.setTheme(getTheme());
    }

    /**
     * {@return the color for a given message}
     */
    public abstract @ColorInt int getMessageNameColor(@NonNull Context context, @NonNull Message message);

    /**
     * {@return the color for a given name}
     */
    public abstract @ColorInt int getMessageColorForName(@NonNull String name);

    /**
     * {@return the icon color for colored icons in list views}
     */
    public @ColorInt int getIconColor(@NonNull Context context, long seed) {
        return Colors.getColor(context, getAccentColorResource(seed));
    }

    /**
     * {@return the color for the pattern in the info bottom sheet header}
     */
    public @ColorInt int getSheetBackgroundColor(@NonNull Context context, long seed) {
        return getIconColor(context, seed);
    }

    /**
     * {@return the color for the dark parts of the pattern in the info bottom sheet header}
     */
    public @ColorInt int getSheetBackgroundColorDark(@NonNull Context context, long seed) {
        return Colors.multiply(getSheetBackgroundColor(context, seed), 0xFFCCCCCC);
    }

    /**
     * {@return the color for the pattern in the info bottom sheet header}
     * @see #getSheetBackgroundColor(Context, long)
     */
    public @ColorInt int getSheetBackgroundColor(@NonNull Context context, @NonNull Message message) {
        return getMessageNameColor(context, message);
    }

    /**
     * {@return the color for the dark parts of the pattern in the info bottom sheet header}
     * @see #getSheetBackgroundColorDark(Context, long)
     */
    public @ColorInt int getSheetBackgroundColorDark(@NonNull Context context, @NonNull Message message) {
        return Colors.multiply(getSheetBackgroundColor(context, message), 0xFFCCCCCC);
    }

    /**
     * {@return the pattern for the info bottom sheet header}
     */
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
