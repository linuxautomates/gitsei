package io.levelops.web.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Objects;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends SpringWebException {
    public NotFoundException() {
    }

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotFoundException(Throwable cause) {
        super(cause);
    }

    public static void checkNotNull(Object arg, String message) throws NotFoundException {
        NotFoundException.check(Objects.nonNull(arg), message);
    }

    public static void check(boolean predicate, String message) throws NotFoundException {
        if (!predicate) {
            throw new NotFoundException(message);
        }
    }
}
