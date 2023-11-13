package io.levelops.lql.exceptions;

public class LqlException extends Exception {

    public LqlException() {
    }

    public LqlException(String message) {
        super(message);
    }

    public LqlException(String message, Throwable cause) {
        super(message, cause);
    }

    public LqlException(Throwable cause) {
        super(cause);
    }
}
