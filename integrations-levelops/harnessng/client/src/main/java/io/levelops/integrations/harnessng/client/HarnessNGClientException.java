package io.levelops.integrations.harnessng.client;

import io.levelops.ingestion.exceptions.FetchException;

/**
 * Exception class to be used for errors with {@link HarnessNGClient}
 */
public class HarnessNGClientException extends FetchException {

    public HarnessNGClientException() {
    }

    public HarnessNGClientException(String message) {
        super(message);
    }

    public HarnessNGClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public HarnessNGClientException(Throwable cause) {
        super(cause);
    }
}
