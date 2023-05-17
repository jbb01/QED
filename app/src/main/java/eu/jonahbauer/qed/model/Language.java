package eu.jonahbauer.qed.model;

import androidx.annotation.StringRes;
import androidx.core.os.LocaleListCompat;
import eu.jonahbauer.qed.R;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Locale;

@Getter
@RequiredArgsConstructor
public enum Language {
    SYSTEM(R.string.language_system, LocaleListCompat.getEmptyLocaleList()),
    GERMAN(R.string.language_german, LocaleListCompat.create(Locale.GERMANY)),
    ENGLISH(R.string.language_english, LocaleListCompat.create(Locale.ENGLISH));

    @StringRes
    private final int name;
    private final LocaleListCompat locales;
}
