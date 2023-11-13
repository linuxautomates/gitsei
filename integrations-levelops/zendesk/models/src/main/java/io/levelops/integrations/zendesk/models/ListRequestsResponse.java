package io.levelops.integrations.zendesk.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Bean for the list requests (https://developer.zendesk.com/rest_api/docs/support/requests#list-requests) api
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ListRequestsResponse.ListRequestsResponseBuilder.class)
public class ListRequestsResponse {

    @JsonProperty("requests")
    List<Ticket.RequestAttributes> requestAttributes;

    @JsonProperty("next_page")
    String nextPage;

    @JsonProperty("previous_page")
    String previousPage;

    @JsonProperty
    int count;
}
