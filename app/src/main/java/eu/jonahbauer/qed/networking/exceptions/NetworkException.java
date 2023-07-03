package eu.jonahbauer.qed.networking.exceptions;

import java.io.IOException;

public class NetworkException extends IOException {
    public NetworkException(Throwable cause) {
        super(cause);
    }
}
