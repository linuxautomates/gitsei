package io.levelops.web.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.LOCKED)
public class LockedException extends SpringWebException {

    public LockedException() {
    }

    public LockedException(String message) {
        super(message);
    }

    public LockedException(String message, Throwable cause) {
        super(message, cause);
    }

    public LockedException(Throwable cause) {
        super(cause);
    }

}
