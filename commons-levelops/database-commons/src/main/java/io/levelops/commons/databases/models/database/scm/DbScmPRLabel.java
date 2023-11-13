package io.levelops.commons.databases.models.database.scm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.github.models.GithubPullRequest;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbScmPRLabel.DbScmPRLabelBuilder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DbScmPRLabel {
    @JsonProperty("id")
    private UUID id;
    @JsonProperty("scm_pullrequest_id")
    private UUID scmPullRequestId;
    @JsonProperty("cloud_id")
    private String cloudId;
    @JsonProperty("name")
    private String name;
    @JsonProperty("description")
    private String description;
    @JsonProperty("label_added_at")
    private Instant labelAddedAt;
    @JsonProperty("label_added_timestamp")
    private Long labelAddedTs;

    public static DbScmPRLabel fromGithubPullRequest(GithubPullRequest.Label label) {
        return DbScmPRLabel.builder()
                .cloudId(label.getId())
                .name(label.getName())
                .description(label.getDescription())
                .build();
    }
}
