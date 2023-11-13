package io.levelops.commons.inventory.exceptions;

public class SecretsManagerServiceClientException extends Exception {
    public SecretsManagerServiceClientException() {
    }

    public SecretsManagerServiceClientException(String message) {
        super(message);
    }

    public SecretsManagerServiceClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public SecretsManagerServiceClientException(Throwable cause) {
        super(cause);
    }
}
