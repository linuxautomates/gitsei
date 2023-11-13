package io.levelops.integrations.gitlab.client;

import io.levelops.ingestion.exceptions.FetchException;

/**
 * Exception class to be used for errors with {@link GitlabClient}
 */
public class GitlabClientException extends FetchException {

    public GitlabClientException() {
    }

    public GitlabClientException(String message) {
        super(message);
    }

    public GitlabClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public GitlabClientException(Throwable cause) {
        super(cause);
    }
}
