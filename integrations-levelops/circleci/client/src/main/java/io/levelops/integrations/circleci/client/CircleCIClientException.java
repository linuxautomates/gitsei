package io.levelops.integrations.circleci.client;

import io.levelops.ingestion.exceptions.FetchException;

/**
 * Exception class to be used for errors with {@link CircleCIClient}
 */
public class CircleCIClientException extends FetchException {

    public CircleCIClientException() {
    }

    public CircleCIClientException(String message) {
        super(message);
    }

    public CircleCIClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public CircleCIClientException(Throwable cause) {
        super(cause);
    }
}
