package io.levelops.commons.faceted_search.db.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class ScmPRUniqId {
    @JsonProperty("project")
    private String project;

    @JsonProperty("number")
    private String number;

    @JsonProperty("integration_id")
    private Integer integrationId;
}
