package com.jonahbauer.qed.util;

import lombok.Data;

@Data
public class StatusWrapper<T> {
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
