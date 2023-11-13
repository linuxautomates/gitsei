package io.levelops.integrations.checkmarx.client.cxsast;

import io.levelops.ingestion.exceptions.FetchException;

public class CxSastClientException extends FetchException {
    private static final long serialVersionUID = -3700911568269219135L;

    public CxSastClientException(String message) {
        super(message);
    }

    public CxSastClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public CxSastClientException(Throwable cause) {
        super(cause);
    }
}
