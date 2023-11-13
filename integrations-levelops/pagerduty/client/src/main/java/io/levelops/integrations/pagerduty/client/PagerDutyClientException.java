package io.levelops.integrations.pagerduty.client;

import io.levelops.ingestion.exceptions.FetchException;

public class PagerDutyClientException extends FetchException {

    /**
     *
     */
    private static final long serialVersionUID = -3322514901252577218L;

    public PagerDutyClientException() {
    }

    public PagerDutyClientException(String message) {
        super(message);
    }

    public PagerDutyClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public PagerDutyClientException(Throwable cause) {
        super(cause);
    }
}