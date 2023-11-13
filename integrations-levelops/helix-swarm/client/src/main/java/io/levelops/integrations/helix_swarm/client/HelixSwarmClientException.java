package io.levelops.integrations.helix_swarm.client;

import io.levelops.ingestion.exceptions.FetchException;

public class HelixSwarmClientException extends FetchException {

    public HelixSwarmClientException() {
    }

    public HelixSwarmClientException(String message) {
        super(message);
    }

    public HelixSwarmClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public HelixSwarmClientException(Throwable cause) {
        super(cause);
    }
}
