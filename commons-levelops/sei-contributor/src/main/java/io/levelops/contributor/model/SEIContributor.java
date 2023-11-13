package io.levelops.contributor.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SEIContributor.SEIContributorBuilder.class)
public class SEIContributor {
    @JsonProperty("org_user_count")
    private Integer orgUserCount;

    @JsonProperty("integration_user_count")
    private Integer integrationUserCount;

    @JsonProperty("org_user_details")
    private List<SEIOrgContributor> orgUserDetailsList;
}
