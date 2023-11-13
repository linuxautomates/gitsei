package io.levelops.notification.models.msteams;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = MSTeamsErrorResponse.MSTeamsErrorResponseBuilder.class)
public class MSTeamsErrorResponse {

    @JsonProperty("error")
    ErrorResponse error;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ErrorResponse.ErrorResponseBuilder.class)
    public static class ErrorResponse {

        @JsonProperty("code")
        String code;

        @JsonProperty("message")
        String message;

    }
}
