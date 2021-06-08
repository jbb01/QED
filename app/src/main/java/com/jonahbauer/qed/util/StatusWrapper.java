package com.jonahbauer.qed.util;

import lombok.Data;

@Data
public class StatusWrapper<T> {
    public static final int STATUS_PRELOADED = 0;
    public static final int STATUS_LOADED = 1;
    public static final int STATUS_ERROR = -1;

    private T value;
    private int code;
    private String message;

    public static <T> StatusWrapper<T> wrap(T value, int code) {
        StatusWrapper<T> out = new StatusWrapper<>(value);
        out.setCode(code);
        return out;
    }

    public static <T> StatusWrapper<T> wrap(T value, int code, String message) {
        StatusWrapper<T> out = new StatusWrapper<>(value);
        out.setCode(code);
        out.setMessage(message);
        return out;
    }

    public StatusWrapper(T value) {
        this.value = value;
    }
}
