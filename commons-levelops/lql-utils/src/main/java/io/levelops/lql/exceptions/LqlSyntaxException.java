package io.levelops.lql.exceptions;

public class LqlSyntaxException extends LqlException {

    public LqlSyntaxException() {
    }

    public LqlSyntaxException(String message) {
        super(message);
    }

    public LqlSyntaxException(String message, Throwable cause) {
        super(message, cause);
    }

    public LqlSyntaxException(Throwable cause) {
        super(cause);
    }
}
