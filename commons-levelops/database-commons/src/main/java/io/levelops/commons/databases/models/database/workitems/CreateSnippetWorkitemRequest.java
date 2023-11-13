package io.levelops.commons.databases.models.database.workitems;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.NotificationMode;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CreateSnippetWorkitemRequest.CreateSnippetWorkitemRequestBuilder.class)
public class CreateSnippetWorkitemRequest {
    @JsonProperty("title")
    private final String title;

    @JsonProperty("url")
    private final String url;

    @JsonProperty("requestor")
    private final String requestor;

    @JsonProperty("recipients")
    private final List<String> recipients;

    @JsonProperty("mode")
    private final NotificationMode mode;

    @JsonProperty("message")
    private final String message;

    @JsonProperty("escalate")
    private final Boolean escalate;
}
