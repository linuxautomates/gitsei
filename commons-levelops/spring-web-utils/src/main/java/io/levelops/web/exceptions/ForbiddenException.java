package io.levelops.web.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Objects;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends SpringWebException {

    public ForbiddenException() {
    }

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }

    public ForbiddenException(Throwable cause) {
        super(cause);
    }

    public static void checkNotNull(Object arg, String message) throws ForbiddenException {
        ForbiddenException.check(Objects.nonNull(arg), message);
    }

    public static void check(boolean predicate, String message) throws ForbiddenException {
        if (!predicate) {
            throw new ForbiddenException(message);
        }
    }
}