package io.levelops.integrations.awsdevtools.client;

import io.levelops.ingestion.exceptions.FetchException;

/**
 * Exception class to be used for errors with {@link AWSDevToolsClient}
 */
public class AWSDevToolsClientException extends FetchException {

    public AWSDevToolsClientException() {
    }

    public AWSDevToolsClientException(String message) {
        super(message);
    }

    public AWSDevToolsClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public AWSDevToolsClientException(Throwable cause) {
        super(cause);
    }
}
