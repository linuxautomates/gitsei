package io.levelops.integrations.postgres.client;

public class PostgresClientException extends Exception {
    public PostgresClientException() {
    }

    public PostgresClientException(String message) {
        super(message);
    }

    public PostgresClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public PostgresClientException(Throwable cause) {
        super(cause);
    }
}
