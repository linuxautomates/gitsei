package io.propelo.trellis_framework.client.exception;

public  class TrellisControllerClientException extends Exception {
    public TrellisControllerClientException() {
    }

    public TrellisControllerClientException(String message) {
        super(message);
    }

    public TrellisControllerClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public TrellisControllerClientException(Throwable cause) {
        super(cause);
    }
}
