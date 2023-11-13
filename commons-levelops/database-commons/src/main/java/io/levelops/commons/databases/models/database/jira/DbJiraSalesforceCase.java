package io.levelops.commons.databases.models.database.jira;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbJiraSalesforceCase.DbJiraSalesforceCaseBuilder.class)
public class DbJiraSalesforceCase {
    @JsonProperty("issue_key")
    String issueKey;

    @JsonProperty("integration_id")
    Integer integrationId;

    @JsonProperty("fieldkey")
    String fieldKey;

    @JsonProperty("fieldvalue")
    String fieldValue;
}
