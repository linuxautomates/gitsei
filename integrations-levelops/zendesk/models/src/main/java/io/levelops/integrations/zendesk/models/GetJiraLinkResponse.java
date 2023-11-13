package io.levelops.integrations.zendesk.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * bean for the list jira link api (https://developer.zendesk.com/rest_api/docs/services/jira#list-links) response
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GetJiraLinkResponse.GetJiraLinkResponseBuilder.class)
public class GetJiraLinkResponse {

    @JsonProperty
    List<JiraLink> links;

    @JsonProperty
    int total;
}
