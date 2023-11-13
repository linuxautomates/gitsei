package io.levelops.integrations.slack.client;

import io.levelops.integrations.slack.models.SlackApiResponse;
import lombok.Getter;

@Getter
public class SlackClientException extends Exception {
    private SlackApiResponse apiResponse;
    public SlackClientException() {
    }

    public SlackClientException(String message, SlackApiResponse apiResponse) {
        super(message);
        this.apiResponse = apiResponse;
    }

    public SlackClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public SlackClientException(Throwable cause) {
        super(cause);
    }
}
