package io.levelops.integrations.zendesk.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * bean for the list request-comments
 * api (https://developer.zendesk.com/rest_api/docs/support/requests#request-comments) response
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = RequestCommentResponse.RequestCommentResponseBuilder.class)
public class RequestCommentResponse {

    @JsonProperty
    List<RequestComment> comments;

    @JsonProperty("next_page")
    String nextPage;

    @JsonProperty
    int count;

}
