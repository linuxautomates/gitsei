package io.levelops.integrations.azureDevops.client;

import io.levelops.ingestion.exceptions.FetchException;

public class AzureDevopsClientException extends FetchException {

    private static final long serialVersionUID = -3700911568269219135L;

    public AzureDevopsClientException() {
    }

    public AzureDevopsClientException(String message) {
        super(message);
    }

    public AzureDevopsClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public AzureDevopsClientException(Throwable cause) {
        super(cause);
    }
}