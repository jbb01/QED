package com.jonahbauer.qed.model.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.jonahbauer.qed.util.StatusWrapper;

import java.util.function.Function;
import java.util.function.Supplier;

public abstract class InfoViewModel<T> extends AndroidViewModel {
    private final @NonNull MutableLiveData<StatusWrapper<T>> mValueStatus = new MutableLiveData<>();
    private final @NonNull MutableLiveData<CharSequence> mTitle = new MutableLiveData<>();
    private final @NonNull MutableLiveData<CharSequence> mToolbarTitle = new MutableLiveData<>();
    private final @NonNull MutableLiveData<T> mValue = new MutableLiveData<>();
    private final @NonNull MutableLiveData<Boolean> mLoading = new MutableLiveData<>();
    private final @NonNull MutableLiveData<Boolean> mError = new MutableLiveData<>();

    public InfoViewModel(@NonNull Application application) {
        super(application);
    }

    protected abstract @Nullable CharSequence getTitle(@NonNull T value);

    protected @Nullable CharSequence getToolbarTitle(@NonNull T value) {
        return getTitle(value);
    }

    protected abstract @Nullable @StringRes Integer getDefaultTitle();

    protected @Nullable @StringRes Integer getDefaultToolbarTitle() {
        return getDefaultTitle();
    }

    protected void submit(@NonNull StatusWrapper<T> status) {
        mValueStatus.setValue(status);
        mValue.setValue(status.getValue());
        mLoading.setValue(status.getCode() == StatusWrapper.STATUS_PRELOADED);
        mError.setValue(status.getCode() == StatusWrapper.STATUS_ERROR);

        var value = status.getValue();
        mTitle.setValue(getTitle(value, this::getTitle, this::getDefaultTitle));
        mToolbarTitle.setValue(getTitle(value, this::getToolbarTitle, this::getDefaultToolbarTitle));
    }

    public @NonNull LiveData<StatusWrapper<T>> getValueStatus() {
        return this.mValueStatus;
    }

    public @NonNull LiveData<T> getValue() {
        return this.mValue;
    }

    public @NonNull LiveData<CharSequence> getTitle() {
        return this.mTitle;
    }

    public @NonNull LiveData<? extends CharSequence> getToolbarTitle() {
        return this.mToolbarTitle;
    }

    public @NonNull LiveData<Boolean> getLoading() {
        return this.mLoading;
    }

    public @NonNull LiveData<Boolean> getError() {
        return this.mError;
    }

    private @Nullable CharSequence getTitle(T value, Function<T, CharSequence> title, Supplier<Integer> defaultTitle) {
        if (value != null) return title.apply(value);
        var defaultTitleRes = defaultTitle.get();
        return defaultTitleRes != null ? getApplication().getText(defaultTitleRes) : null;
    }
}
