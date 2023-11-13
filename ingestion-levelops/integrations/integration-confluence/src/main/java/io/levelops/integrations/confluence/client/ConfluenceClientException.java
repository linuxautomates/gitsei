package io.levelops.integrations.confluence.client;

public class ConfluenceClientException extends Exception {
    public ConfluenceClientException() {
    }

    public ConfluenceClientException(String message) {
        super(message);
    }

    public ConfluenceClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfluenceClientException(Throwable cause) {
        super(cause);
    }
}
