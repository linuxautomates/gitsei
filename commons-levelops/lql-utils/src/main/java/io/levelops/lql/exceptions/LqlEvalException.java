package io.levelops.lql.exceptions;

public class LqlEvalException extends LqlException {

    public LqlEvalException() {
    }

    public LqlEvalException(String message) {
        super(message);
    }

    public LqlEvalException(String message, Throwable cause) {
        super(message, cause);
    }

    public LqlEvalException(Throwable cause) {
        super(cause);
    }
}
