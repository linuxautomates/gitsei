package io.levelops.exceptions;

import java.io.IOException;

public class InternalApiClientException extends IOException {
    public InternalApiClientException() {
    }

    public InternalApiClientException(String message) {
        super(message);
    }

    public InternalApiClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public InternalApiClientException(Throwable cause) {
        super(cause);
    }
}
