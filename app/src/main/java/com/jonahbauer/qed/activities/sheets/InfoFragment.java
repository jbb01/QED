package com.jonahbauer.qed.activities.sheets;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.jonahbauer.qed.util.ViewUtils;

public abstract class InfoFragment extends Fragment {
    @ColorInt
    public abstract int getColor();

    @DrawableRes
    protected abstract int getBackground();

    protected abstract CharSequence getTitle();

    protected abstract float getTitleBottom();

    public abstract void hideTitle();

    protected ViewModelProvider getViewModelProvider(@IdRes int destinationId) {
        if (destinationId != 0) try {
            return ViewUtils.getViewModelProvider(this, destinationId);
        } catch (Exception ignored) {}

        assert getParentFragment() != null;
        return new ViewModelProvider(getParentFragment());
    }
}
