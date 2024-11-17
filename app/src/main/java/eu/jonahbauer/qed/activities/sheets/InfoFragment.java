package eu.jonahbauer.qed.activities.sheets;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import androidx.annotation.ColorInt;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.ui.themes.Theme;
import eu.jonahbauer.qed.ui.views.ListItem;
import eu.jonahbauer.qed.util.Actions;
import eu.jonahbauer.qed.util.ViewUtils;

import java.util.Collection;
import java.util.function.BiConsumer;

public abstract class InfoFragment extends Fragment implements MenuProvider {

    public @ColorInt int getColor() {
        return Theme.getCurrentTheme().getIconColor(requireContext(), getDesignSeed());
    }

    protected abstract long getDesignSeed();

    public boolean hasMenu() {
        return isOpenInBrowserSupported();
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        if (isOpenInBrowserSupported()) {
            var item = menu.add(Menu.NONE, R.id.open_in_browser, Menu.NONE, R.string.open_in_browser);
            item.setIcon(R.drawable.ic_menu_launch);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.open_in_browser && isOpenInBrowserSupported()) {
            Actions.openInBrowser(requireContext(), getOpenInBrowserLink());
            return true;
        }
        return false;
    }

    protected boolean isOpenInBrowserSupported() {
        return false;
    }

    protected @Nullable String getOpenInBrowserLink() {
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
