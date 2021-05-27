package com.jonahbauer.qed.networking.exceptions;

public class InvalidCredentialsException extends Exception {
    public InvalidCredentialsException() {
        super("Failed to login: Invalid credentials.");
    }

    public InvalidCredentialsException(Throwable cause) {
        super("Failed to login: Invalid credentials.", cause);
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
