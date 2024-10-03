package eu.jonahbauer.qed.model.parser;

import androidx.annotation.NonNull;
import eu.jonahbauer.qed.model.Registration;

public final class RegistrationStatusParser {
    public static final RegistrationStatusParser INSTANCE = new RegistrationStatusParser();

    private static final String STATUS_PENDING = "angemeldet";
    private static final String STATUS_CONFIRMED = "bestätigt";
    private static final String STATUS_REJECTED = "abgelehnt";
    private static final String STATUS_CANCELLED = "abgesagt";

    private RegistrationStatusParser() {}

    /**
     * Checks whether the string contains a registration status. If the string does not contain a registration status,
     * the fallback value is returned. If multiple registrations statuses are contained in the given string, the result
     * may be any one of them.
     */
    public @NonNull Registration.Status parse(@NonNull String string) {
        if (string.contains(STATUS_PENDING)) {
            return Registration.Status.PENDING;
        } else if (string.contains(STATUS_CONFIRMED)) {
            return Registration.Status.CONFIRMED;
        } else if (string.contains(STATUS_REJECTED)) {
            return Registration.Status.REJECTED;
        } else if (string.contains(STATUS_CANCELLED)) {
            return Registration.Status.CANCELLED;
        } else {
            return Registration.Status.PENDING;
        }
    }
}
