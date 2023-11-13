package io.levelops.ingestion.exceptions;

public class FetchException extends IngestException {

    public FetchException() {
    }

    public FetchException(String message) {
        super(message);
    }

    public FetchException(String message, Throwable cause) {
        super(message, cause);
    }

    public FetchException(Throwable cause) {
        super(cause);
    }

}
