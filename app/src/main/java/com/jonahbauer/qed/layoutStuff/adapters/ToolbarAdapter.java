package com.jonahbauer.qed.layoutStuff.adapters;

import androidx.appcompat.widget.Toolbar;
import androidx.databinding.BindingAdapter;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ToolbarAdapter {
    @BindingAdapter("title")
    public static void bindTitle(Toolbar toolbar, String title) {
        toolbar.setTitle(title);
    }
}
