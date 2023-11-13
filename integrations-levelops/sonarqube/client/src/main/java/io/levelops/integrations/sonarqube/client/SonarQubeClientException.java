package io.levelops.integrations.sonarqube.client;

import io.levelops.ingestion.exceptions.FetchException;

/**
 * Exception class to be used for errors with {@link SonarQubeClient}
 */
public class SonarQubeClientException extends FetchException {

    private static final long serialVersionUID = -3700911568269219135L;

    public SonarQubeClientException() {
    }

    public SonarQubeClientException(String message) {
        super(message);
    }

    public SonarQubeClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public SonarQubeClientException(Throwable cause) {
        super(cause);
    }
}