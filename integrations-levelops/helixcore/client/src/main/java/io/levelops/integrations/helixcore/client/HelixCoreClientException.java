package io.levelops.integrations.helixcore.client;

import io.levelops.ingestion.exceptions.FetchException;

public class HelixCoreClientException extends FetchException {
    public HelixCoreClientException() {
    }

    public HelixCoreClientException(String message) {
        super(message);
    }

    public HelixCoreClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public HelixCoreClientException(Throwable cause) {
        super(cause);
    }
}
