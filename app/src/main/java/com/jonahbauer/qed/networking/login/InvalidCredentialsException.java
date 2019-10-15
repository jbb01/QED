package com.jonahbauer.qed.networking.login;

public class InvalidCredentialsException extends Exception {
    public InvalidCredentialsException() {
        super("Failed to login: Invalid credentials.");
    }
    public InvalidCredentialsException(Throwable cause) {
        super("Failed to login: Invalid credentials.", cause);
    }
}
