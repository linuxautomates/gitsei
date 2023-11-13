package io.levelops.integrations.tenable.client;

import io.levelops.ingestion.exceptions.FetchException;

public class TenableClientException extends FetchException {
    public TenableClientException() {
    }

    public TenableClientException(String message) {
        super(message);
    }

    public TenableClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public TenableClientException(Throwable cause) {
        super(cause);
    }
}

