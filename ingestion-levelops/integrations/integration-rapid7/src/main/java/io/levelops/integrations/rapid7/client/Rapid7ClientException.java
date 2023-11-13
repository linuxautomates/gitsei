package io.levelops.integrations.rapid7.client;

public class Rapid7ClientException extends Exception {
    public Rapid7ClientException() {
    }

    public Rapid7ClientException(String message) {
        super(message);
    }

    public Rapid7ClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public Rapid7ClientException(Throwable cause) {
        super(cause);
    }
}
