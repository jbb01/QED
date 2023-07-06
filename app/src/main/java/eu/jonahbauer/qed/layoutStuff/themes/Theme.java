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
}
