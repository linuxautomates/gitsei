package io.levelops.integrations.salesforce.client;

import io.levelops.ingestion.exceptions.FetchException;

public class SalesforceClientException extends FetchException {
    /**
     *
     */
    private static final long serialVersionUID = 6054801519452663070L;

    public SalesforceClientException() {
    }

    public SalesforceClientException(String message) {
        super(message);
    }

    public SalesforceClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public SalesforceClientException(Throwable cause) {
        super(cause);
    }
}
