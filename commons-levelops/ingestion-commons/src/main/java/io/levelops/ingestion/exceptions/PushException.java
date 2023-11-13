package io.levelops.ingestion.exceptions;

public class PushException extends IngestException {
    public PushException() {
    }

    public PushException(String message) {
        super(message);
    }

    public PushException(String message, Throwable cause) {
        super(message, cause);
    }

    public PushException(Throwable cause) {
        super(cause);
    }
}
