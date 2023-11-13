package io.levelops.web.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Objects;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends SpringWebException {

    public BadRequestException() {
    }

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadRequestException(Throwable cause) {
        super(cause);
    }

    public static void checkNotNull(Object arg, String message) throws BadRequestException {
        BadRequestException.check(Objects.nonNull(arg), message);
    }

    public static void check(boolean predicate, String message) throws BadRequestException {
        if (!predicate) {
            throw new BadRequestException(message);
        }
    }
}
