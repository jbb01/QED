package com.jonahbauer.qed.layoutStuff.views;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.databinding.ListItemBinding;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.appcompat.content.res.AppCompatResources;

public class ListItem extends LinearLayout {

    private final ListItemBinding mBinding;

    private CharSequence mTitle;
    private CharSequence mSubtitle;
    private Drawable mIcon;

    public ListItem(@NonNull Context context) {
        this(context, null);
    }

    public ListItem(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ListItem(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        this(context, attrs, defStyleAttr, R.style.Widget_App_ListItem);
    }

    public ListItem(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        mBinding = ListItemBinding.inflate(LayoutInflater.from(context), this);

        var array = context.obtainStyledAttributes(attrs, R.styleable.ListItem, defStyleAttr, defStyleRes);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveAttributeDataForStyleable(context, R.styleable.ListItem, attrs, array, defStyleAttr, defStyleRes);
        }

        if (array.hasValue(R.styleable.ListItem_title)) {
            setTitle(array.getText(R.styleable.ListItem_title));
        } else {
            setTitle(null);
        }

        if (array.hasValue(R.styleable.ListItem_subtitle)) {
            setSubtitle(array.getText(R.styleable.ListItem_subtitle));
        } else {
            setSubtitle(null);
        }

        if (array.hasValue(R.styleable.ListItem_icon)) {
            setIcon(array.getDrawable(R.styleable.ListItem_icon));
        }

        if (array.hasValue(R.styleable.ListItem_iconTint)) {
            setIconTint(array.getColorStateList(R.styleable.ListItem_iconTint));
        }

        array.recycle();
    }

    public CharSequence getTitle() {
        return mTitle;
    }

    public void setTitle(CharSequence title) {
        mTitle = title;
        mBinding.title.setText(title);
    }

    public void setTitle(@StringRes int title) {
        setTitle(title == 0 ? null : getResources().getText(title));
    }

    public CharSequence getSubtitle() {
        return mSubtitle;
    }

    public void setSubtitle(CharSequence subtitle) {
        mSubtitle = subtitle;
        mBinding.subtitle.setVisibility(subtitle == null ? View.GONE : View.VISIBLE);
        mBinding.subtitle.setText(subtitle);
    }

    public void setSubtitle(@StringRes int subtitle) {
        setSubtitle(subtitle == 0 ? null : getResources().getText(subtitle));
    }

    public Drawable getIcon() {
        return mIcon;
    }

    public void setIcon(Drawable icon) {
        mIcon = icon;
        mBinding.icon.setImageDrawable(icon);
    }

    public void setIcon(@DrawableRes int icon) {
        setIcon(icon == 0 ? null : AppCompatResources.getDrawable(getContext(), icon));
    }

    public void setIconTint(ColorStateList tint) {
        mBinding.icon.setImageTintList(tint);
    }

    public void setIconTint(@ColorInt int tint) {
        setIconTint(ColorStateList.valueOf(tint));
    }

    public void setTitleTextAppearance(@StyleRes int textAppearance) {
        mBinding.title.setTextAppearance(textAppearance);
    }

    public void setTitleTextColor(@ColorInt int color) {
        mBinding.title.setTextColor(color);
    }
}
