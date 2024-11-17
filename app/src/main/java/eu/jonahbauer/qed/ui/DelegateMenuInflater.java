package eu.jonahbauer.qed.ui;

import android.view.Menu;
import android.view.MenuInflater;
import androidx.appcompat.widget.Toolbar;

/**
 * A {@link MenuInflater} that delegates menu inflation to a {@link Toolbar} where appropriate.
 */
public class DelegateMenuInflater extends MenuInflater {
    private final Toolbar mToolbar;

    public DelegateMenuInflater(Toolbar toolbar) {
        super(toolbar.getContext());
        this.mToolbar = toolbar;
    }

    @Override
    public void inflate(int menuRes, Menu menu) {
        if (menu == mToolbar.getMenu()) {
            mToolbar.inflateMenu(menuRes);
        } else {
            super.inflate(menuRes, menu);
        }
    }
}
