package com.jonahbauer.qed.layoutStuff;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.StringRes;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;

public class CustomActionMode extends ActionMode implements Toolbar.OnMenuItemClickListener {
    private final Toolbar mToolbar;
    private final MenuInflater mMenuInflater;
    private boolean mFinished;

    private final ActionMode.Callback mCallback;

    public CustomActionMode(Toolbar toolbar, Callback callback) {
        this.mCallback = callback;

        this.mToolbar = toolbar;
        this.mToolbar.getMenu().clear();
        this.mToolbar.setOnMenuItemClickListener(this);
        this.mMenuInflater = new DelegateMenuInflater(toolbar);
    }

    @Override
    public void setTitle(CharSequence title) {
        mToolbar.setTitle(title);
    }

    @Override
    public void setTitle(@StringRes int resId) {
        mToolbar.setTitle(resId);
    }

    @Override
    public void setSubtitle(CharSequence subtitle) {
        mToolbar.setSubtitle(subtitle);
    }

    @Override
    public void setSubtitle(@StringRes int resId) {
        mToolbar.setSubtitle(resId);
    }

    @Override
    public void setCustomView(View view) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void invalidate() {
        mCallback.onPrepareActionMode(this, mToolbar.getMenu());
    }

    @Override
    public void finish() {
        if (mFinished) {
            return;
        }
        mFinished = true;

        mCallback.onDestroyActionMode(this);
        mToolbar.setOnMenuItemClickListener(null);
    }

    @Override
    public Menu getMenu() {
        return mToolbar.getMenu();
    }

    @Override
    public CharSequence getTitle() {
        return mToolbar.getTitle();
    }

    @Override
    public CharSequence getSubtitle() {
        return mToolbar.getSubtitle();
    }

    @Override
    public View getCustomView() {
        throw new UnsupportedOperationException();
    }

    @Override
    public MenuInflater getMenuInflater() {
        return mMenuInflater;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return mCallback.onActionItemClicked(this, item);
    }
}
