package com.jonahbauer.qed.layoutStuff.adapters;

import android.content.res.ColorStateList;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.core.widget.TextViewCompat;
import androidx.databinding.BindingAdapter;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TextViewAdapter {
    @BindingAdapter("drawableTint")
    public static void setDrawableTint(TextView textView, @ColorInt int color) {
        TextViewCompat.setCompoundDrawableTintList(textView, ColorStateList.valueOf(color));
    }
}
