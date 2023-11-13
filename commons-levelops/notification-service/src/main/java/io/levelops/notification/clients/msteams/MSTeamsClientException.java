package io.levelops.notification.clients.msteams;

import lombok.Getter;

@Getter
public class MSTeamsClientException extends Exception {


    public MSTeamsClientException() {
    }

    public MSTeamsClientException(String message) {
        super(message);
    }

    public MSTeamsClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public MSTeamsClientException(Throwable cause) {
        super(cause);
    }
}
