package eu.jonahbauer.qed.model.parser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import eu.jonahbauer.qed.model.Registration;
import org.jetbrains.annotations.Contract;

public final class RegistrationStatusParser {
    public static final RegistrationStatusParser INSTANCE = new RegistrationStatusParser();

    private static final String STATUS_PENDING = "offen";
    private static final String STATUS_CONFIRMED = "bestätigt";
    private static final String STATUS_REJECTED = "abgelehnt";
    private static final String STATUS_CANCELLED = "abgesagt";

    private RegistrationStatusParser() {}

    /**
     * Parses the complete string as a registration status. Returns {@code null} if the string could not
     * be parsed as a registration status.
     */
    public @Nullable Registration.Status parse(@NonNull String string) {
        switch (string) {
            case STATUS_PENDING: return Registration.Status.PENDING;
            case STATUS_CONFIRMED: return Registration.Status.CONFIRMED;
            case STATUS_REJECTED: return Registration.Status.REJECTED;
            case STATUS_CANCELLED: return Registration.Status.CANCELLED;
            default: return null;
        }
    }

    /**
     * Checks whether the string contains a registration status. If the string does not contain a registration status,
     * the fallback value is returned. If multiple registrations statuses are contained in the given string, the result
     * may be any one of them.
     */
    @Contract("_, !null -> !null")
    public @Nullable Registration.Status parseLenient(@NonNull String string, Registration.Status fallback) {
        if (string.contains(STATUS_PENDING)) {
            return Registration.Status.PENDING;
        } else if (string.contains(STATUS_CONFIRMED)) {
            return Registration.Status.CONFIRMED;
        } else if (string.contains(STATUS_REJECTED)) {
            return Registration.Status.REJECTED;
        } else if (string.contains(STATUS_CANCELLED)) {
            return Registration.Status.CANCELLED;
        } else {
            return fallback;
        }
    }
}
