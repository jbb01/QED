package eu.jonahbauer.qed.ui.binding;

import android.content.res.ColorStateList;

import androidx.appcompat.widget.Toolbar;
import androidx.databinding.BindingAdapter;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ToolbarAdapter {
    @BindingAdapter("title")
    public static void bindTitle(Toolbar toolbar, String title) {
        toolbar.setTitle(title);
    }

    @BindingAdapter("titleTextColor")
    public static void bindTitleTextColor(Toolbar toolbar, ColorStateList color) {
        toolbar.setTitleTextColor(color);
    }
}
