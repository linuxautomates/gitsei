package io.levelops.web.exceptions;

public abstract class SpringWebException extends Exception {

    public SpringWebException() {
    }

    public SpringWebException(String message) {
        super(message);
    }

    public SpringWebException(String message, Throwable cause) {
        super(message, cause);
    }

    public SpringWebException(Throwable cause) {
        super(cause);
    }
}
