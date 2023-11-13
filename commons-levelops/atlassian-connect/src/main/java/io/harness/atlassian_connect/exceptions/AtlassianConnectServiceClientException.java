package io.harness.atlassian_connect.exceptions;

public class AtlassianConnectServiceClientException extends Exception {
    public AtlassianConnectServiceClientException() {
    }

    public AtlassianConnectServiceClientException(String message) {
        super(message);
    }

    public AtlassianConnectServiceClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public AtlassianConnectServiceClientException(Throwable cause) {
        super(cause);
    }
}
