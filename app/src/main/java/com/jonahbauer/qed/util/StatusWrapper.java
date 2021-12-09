package com.jonahbauer.qed.util;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.networking.Reason;

import lombok.Data;

@Data
public class StatusWrapper<T> {
    @IntDef({STATUS_ERROR, STATUS_LOADED, STATUS_PRELOADED})
    public @interface Status {}

    public static final int STATUS_PRELOADED = 0;
    public static final int STATUS_LOADED = 1;
    public static final int STATUS_ERROR = -1;

    private T value;

    @Status
    private int code;

    private Reason reason;

    public static <T> StatusWrapper<T> preloaded(T value) {
        return wrap(value, STATUS_PRELOADED, null);
    }

    public static <T> StatusWrapper<T> loaded(T value) {
        return wrap(value, STATUS_LOADED, null);
    }

    public static <T> StatusWrapper<T> error(T value, Throwable e) {
        return wrap(value, STATUS_ERROR, Reason.guess(e));
    }

    public static <T> StatusWrapper<T> error(T value, @NonNull Reason reason) {
        return wrap(value, STATUS_ERROR, reason);
    }

    public static <T> StatusWrapper<T> wrap(T value, @Status int code, Reason message) {
        StatusWrapper<T> out = new StatusWrapper<>(value);
        out.setCode(code);
        out.setReason(message);
        return out;
    }

    public StatusWrapper(T value) {
        this.value = value;
    }

    @StringRes
    public int getErrorMessage() {
        if (reason != null) return reason.getStringRes();
        else return R.string.empty;
    }
}
