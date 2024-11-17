package eu.jonahbauer.qed.ui.binding;

import android.content.res.ColorStateList;
import android.widget.ProgressBar;

import androidx.annotation.ColorInt;
import androidx.databinding.BindingAdapter;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ProgressBarAdapter {
    @BindingAdapter("indeterminateTint")
    public static void setIndeterminateTint(ProgressBar progressBar, @ColorInt int color) {
        progressBar.setIndeterminateTintList(ColorStateList.valueOf(color));
    }
}
