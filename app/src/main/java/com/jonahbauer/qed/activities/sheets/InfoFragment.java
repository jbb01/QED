package com.jonahbauer.qed.activities.sheets;

import android.view.ViewGroup;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.jonahbauer.qed.layoutStuff.views.ListItem;
import com.jonahbauer.qed.util.ViewUtils;

import java.util.Collection;
import java.util.function.BiConsumer;

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

    protected static <T> void bindList(@NonNull ViewGroup parent, @NonNull Collection<T> items, @NonNull BiConsumer<T, ListItem> updateView) {
        var context = parent.getContext();
        var childCount = parent.getChildCount();
        var count = items.size();
        if (childCount > count) {
            parent.removeViews(count, childCount - count);
            childCount = count;
        }

        var iterator = items.iterator();
        int i = 0;
        for (; i < childCount; i++) {
            updateView.accept(iterator.next(), (ListItem) parent.getChildAt(i));
        }
        for (; i < count; i++) {
            var view = new ListItem(context);
            updateView.accept(iterator.next(), view);
            parent.addView(view);
        }
    }
}
