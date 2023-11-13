package io.levelops.normalization.exceptions;

public class NormalizationException extends Exception {

    public NormalizationException() {
    }

    public NormalizationException(String message) {
        super(message);
    }

    public NormalizationException(String message, Throwable cause) {
        super(message, cause);
    }

    public NormalizationException(Throwable cause) {
        super(cause);
    }
}
