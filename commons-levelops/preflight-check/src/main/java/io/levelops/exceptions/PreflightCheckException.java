package io.levelops.exceptions;

public class PreflightCheckException extends Exception {
    public PreflightCheckException() {
    }

    public PreflightCheckException(String message) {
        super(message);
    }

    public PreflightCheckException(String message, Throwable cause) {
        super(message, cause);
    }

    public PreflightCheckException(Throwable cause) {
        super(cause);
    }
}
