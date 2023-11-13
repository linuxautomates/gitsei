package io.levelops.exceptions;

public class IngestionPushClientException extends Exception {
    public IngestionPushClientException() {
    }

    public IngestionPushClientException(String message) {
        super(message);
    }

    public IngestionPushClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public IngestionPushClientException(Throwable cause) {
        super(cause);
    }
}