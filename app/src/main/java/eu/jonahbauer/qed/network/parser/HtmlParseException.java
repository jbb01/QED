package eu.jonahbauer.qed.network.parser;

import eu.jonahbauer.qed.networking.Reason;

import lombok.Getter;

@Getter
public class HtmlParseException extends RuntimeException {
    private final Reason reason;

    public HtmlParseException() {
        this.reason = Reason.UNKNOWN;
    }

    public HtmlParseException(String message) {
        super(message);
        this.reason = Reason.UNKNOWN;
    }

    public HtmlParseException(Reason reason) {
        this.reason = reason;
    }

    public HtmlParseException(String message, Reason reason) {
        super(message);
        this.reason = reason;
    }
}
