package io.levelops.integrations.gerrit.client;

import io.levelops.ingestion.exceptions.FetchException;

public class GerritClientException extends FetchException {

    public GerritClientException() {
    }

    public GerritClientException(String message) {
        super(message);
    }

    public GerritClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public GerritClientException(Throwable cause) {
        super(cause);
    }
}
