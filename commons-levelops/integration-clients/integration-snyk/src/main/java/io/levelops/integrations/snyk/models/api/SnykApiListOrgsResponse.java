package io.levelops.integrations.snyk.models.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.snyk.models.SnykOrg;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SnykApiListOrgsResponse.SnykApiListOrgsResponseBuilder.class)
public class SnykApiListOrgsResponse {
    //https://snyk.docs.apiary.io/#reference/organizations/the-snyk-organization-for-a-request/list-all-the-organizations-a-user-belongs-to

    @JsonProperty("orgs")
    private final List<SnykOrg> orgs;
}
