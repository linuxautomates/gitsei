package io.levelops.integrations.blackduck;

import io.levelops.ingestion.exceptions.FetchException;

/**
 * Exception class to be used for errors with {@link BlackDuckClient}
 */

public class BlackDuckClientException extends FetchException {
    public BlackDuckClientException() {
    }

    public BlackDuckClientException(String message) {
        super(message);
    }

    public BlackDuckClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public BlackDuckClientException(Throwable cause) {
        super(cause);
    }
}
