package io.levelops.integrations.okta.client;

import io.levelops.ingestion.exceptions.FetchException;

/**
 * Exception class to be used for errors with {@link OktaClient}
 */
public class OktaClientException extends FetchException {

    public OktaClientException() {
    }

    public OktaClientException(String message) {
        super(message);
    }

    public OktaClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public OktaClientException(Throwable cause) {
        super(cause);
    }
}
