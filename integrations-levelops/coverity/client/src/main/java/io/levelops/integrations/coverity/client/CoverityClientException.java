package io.levelops.integrations.coverity.client;

import io.levelops.ingestion.exceptions.FetchException;

/**
 * Exception class to be used for errors with {@link CoverityClient}
 */
public class CoverityClientException extends FetchException {

    /**
     *
     */
    private static final long serialVersionUID = -3700911568269219135L;

    public CoverityClientException() {
    }

    public CoverityClientException(String message) {
        super(message);
    }

    public CoverityClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public CoverityClientException(Throwable cause) {
        super(cause);
    }

}