package io.levelops.ingestion.exceptions;

public class IngestionServiceException extends Exception {
    public IngestionServiceException() {
    }

    public IngestionServiceException(String message) {
        super(message);
    }

    public IngestionServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public IngestionServiceException(Throwable cause) {
        super(cause);
    }
}
