package io.levelops.integrations.testrails.client;

import io.levelops.ingestion.exceptions.FetchException;

/**
 * Exception class to be used for errors with {@link TestRailsClient}
 */
public class TestRailsClientException extends FetchException {

    public TestRailsClientException() {
    }

    public TestRailsClientException(String message) {
        super(message);
    }

    public TestRailsClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public TestRailsClientException(Throwable cause) {
        super(cause);
    }
}
