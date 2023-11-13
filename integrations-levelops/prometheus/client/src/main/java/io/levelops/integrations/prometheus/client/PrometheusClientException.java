package io.levelops.integrations.prometheus.client;

import io.levelops.ingestion.exceptions.FetchException;

public class PrometheusClientException extends FetchException {

    public PrometheusClientException() {
    }

    public PrometheusClientException(String message) {
        super(message);
    }

    public PrometheusClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public PrometheusClientException(Throwable cause) {
        super(cause);
    }
}
