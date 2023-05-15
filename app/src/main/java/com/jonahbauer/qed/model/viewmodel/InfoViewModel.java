package com.jonahbauer.qed.model.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.jonahbauer.qed.util.StatusWrapper;

public abstract class InfoViewModel<T> extends AndroidViewModel {
    private final @NonNull MutableLiveData<StatusWrapper<T>> mValueStatus = new MutableLiveData<>();
    private final @NonNull MutableLiveData<CharSequence> mTitle = new MutableLiveData<>();
    private final @NonNull MutableLiveData<T> mValue = new MutableLiveData<>();
    private final @NonNull MutableLiveData<Boolean> mLoading = new MutableLiveData<>();
    private final @NonNull MutableLiveData<Boolean> mError = new MutableLiveData<>();

    public InfoViewModel(@NonNull Application application) {
        super(application);
    }

    protected abstract @NonNull CharSequence getTitle(@NonNull T t);

    protected abstract @StringRes int getDefaultTitle();

    protected void submit(@NonNull StatusWrapper<T> status) {
        mValueStatus.setValue(status);
        mValue.setValue(status.getValue());
        mLoading.setValue(status.getCode() == StatusWrapper.STATUS_PRELOADED);
        mError.setValue(status.getCode() == StatusWrapper.STATUS_ERROR);

        var value = status.getValue();
        mTitle.setValue(value == null ? getApplication().getString(getDefaultTitle()) : getTitle(value));
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

    public @NonNull LiveData<Boolean> getLoading() {
        return this.mLoading;
    }

    public @NonNull LiveData<Boolean> getError() {
        return this.mError;
    }
}
