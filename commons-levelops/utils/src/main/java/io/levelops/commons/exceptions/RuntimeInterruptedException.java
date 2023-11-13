package io.levelops.commons.exceptions;

public class RuntimeInterruptedException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = -3563118904405053499L;

    public static RuntimeInterruptedException propagate(Throwable cause) {
        Thread.currentThread().interrupt();
        return new RuntimeInterruptedException("Interrupted", cause);
    }

    public RuntimeInterruptedException() {
    }

    public RuntimeInterruptedException(String message) {
        super(message);
    }

    public RuntimeInterruptedException(String message, Throwable cause) {
        super(message, cause);
    }

    public RuntimeInterruptedException(Throwable cause) {
        super(cause);
    }
}
