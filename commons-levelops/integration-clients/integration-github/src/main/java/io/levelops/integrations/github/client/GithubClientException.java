package io.levelops.integrations.github.client;

public class GithubClientException extends Exception {

    public GithubClientException() {
    }

    public GithubClientException(String message) {
        super(message);
    }

    public GithubClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public GithubClientException(Throwable cause) {
        super(cause);
    }
}
