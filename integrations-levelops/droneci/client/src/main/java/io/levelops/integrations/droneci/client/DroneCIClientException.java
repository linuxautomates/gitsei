package io.levelops.integrations.droneci.client;

import io.levelops.ingestion.exceptions.FetchException;

/**
 * Exception class to be used for errors with {@link DroneCIClient}
 */
public class DroneCIClientException extends FetchException {

    public DroneCIClientException() {
    }

    public DroneCIClientException(String message) {
        super(message);
    }

    public DroneCIClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public DroneCIClientException(Throwable cause) {
        super(cause);
    }
}
