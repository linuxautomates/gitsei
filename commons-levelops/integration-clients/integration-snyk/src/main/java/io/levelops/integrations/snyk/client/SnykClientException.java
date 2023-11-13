package io.levelops.integrations.snyk.client;

public class SnykClientException extends Exception {
    public SnykClientException() {
    }

    public SnykClientException(String message) {
        super(message);
    }

    public SnykClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public SnykClientException(Throwable cause) {
        super(cause);
    }
}