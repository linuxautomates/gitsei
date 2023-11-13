package io.levelops.commons.token_exceptions;

public class UnknownTokenException extends TokenException {
    public UnknownTokenException(String message) {
        super(message);
    }

    public UnknownTokenException(Exception e) {
        super(e);
    }
}
