package eu.jonahbauer.qed.activities.sheets;

import android.view.ViewGroup;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import eu.jonahbauer.qed.layoutStuff.themes.Theme;
import eu.jonahbauer.qed.layoutStuff.views.ListItem;
import eu.jonahbauer.qed.util.ViewUtils;

import java.util.Collection;
import java.util.function.BiConsumer;

public abstract class InfoFragment extends Fragment {

    public @ColorInt int getColor() {
        return Theme.getCurrentTheme().getIconColor(requireContext(), getDesignSeed());
    }

    protected abstract long getDesignSeed();

    public boolean isOpenInBrowserSupported() {
        return false;
    }

    public @Nullable String getOpenInBrowserLink() {
        return null;
    }

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
