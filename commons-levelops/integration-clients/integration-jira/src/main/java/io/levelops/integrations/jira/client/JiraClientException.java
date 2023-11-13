package io.levelops.integrations.jira.client;

public class JiraClientException extends Exception {
    public JiraClientException() {
    }

    public JiraClientException(String message) {
        super(message);
    }

    public JiraClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public JiraClientException(Throwable cause) {
        super(cause);
    }
}
