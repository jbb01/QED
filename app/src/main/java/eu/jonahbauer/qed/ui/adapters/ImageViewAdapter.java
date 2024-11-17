package eu.jonahbauer.qed.ui.adapters;

import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.databinding.BindingAdapter;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ImageViewAdapter {
    @BindingAdapter("tint")
    public static void setImageTint(ImageView imageView, @ColorInt int color) {
        imageView.setColorFilter(color);
    }
}
