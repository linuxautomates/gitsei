package io.levelops.integrations.checkmarx.client.cxsca;

import io.levelops.ingestion.exceptions.FetchException;

public class CxScaClientException extends FetchException {
    private static final long serialVersionUID = -3700911568269219135L;

    public CxScaClientException(String message) {
        super(message);
    }

    public CxScaClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public CxScaClientException(Throwable cause) {
        super(cause);
    }
}
