package io.levelops.integrations.zendesk.client;

import io.levelops.ingestion.exceptions.FetchException;

/**
 * Exception class to be used for errors with {@link ZendeskClient}
 */
public class ZendeskClientException extends FetchException {

    /**
     *
     */
    private static final long serialVersionUID = -3700911568269219135L;

    public ZendeskClientException() {
    }

    public ZendeskClientException(String message) {
        super(message);
    }

    public ZendeskClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public ZendeskClientException(Throwable cause) {
        super(cause);
    }

}