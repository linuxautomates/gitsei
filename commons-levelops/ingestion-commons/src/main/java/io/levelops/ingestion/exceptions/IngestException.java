package io.levelops.ingestion.exceptions;

public class IngestException extends Exception {
    public IngestException() {
    }

    public IngestException(String message) {
        super(message);
    }

    public IngestException(String message, Throwable cause) {
        super(message, cause);
    }

    public IngestException(Throwable cause) {
        super(cause);
    }
}
