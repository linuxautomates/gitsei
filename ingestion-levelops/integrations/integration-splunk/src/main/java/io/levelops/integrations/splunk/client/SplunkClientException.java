package io.levelops.integrations.splunk.client;

public class SplunkClientException extends Exception {
    public SplunkClientException() {
    }

    public SplunkClientException(String message) {
        super(message);
    }

    public SplunkClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public SplunkClientException(Throwable cause) {
        super(cause);
    }
}
