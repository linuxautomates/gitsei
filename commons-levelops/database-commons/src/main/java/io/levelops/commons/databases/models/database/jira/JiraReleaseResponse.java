package io.levelops.commons.databases.models.database.jira;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.jackson.DefaultObjectMapper;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;

@Value
@Log4j2
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraReleaseResponse.JiraReleaseResponseBuilder.class)
public class JiraReleaseResponse {

    @JsonProperty("name")
    String name;

    @JsonProperty("average_lead_time")
    Long averageTime;

    @JsonProperty("issue_count")
    Integer issueCount;

    @JsonProperty("project")
    String project;

    @JsonProperty("released_end_time")
    Long releaseEndTime;

    public String toString() {
        return DefaultObjectMapper.writeAsPrettyJson(this);
    }
}
