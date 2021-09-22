package com.jonahbauer.qed.networking.exceptions;

public class LowMemoryException extends RuntimeException {
    public LowMemoryException(String message) {
        super(message);
    }
}
