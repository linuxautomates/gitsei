package io.levelops.commons.databases.models.database.scm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbScmCommitPRMapping.DbScmCommitPRMappingBuilder.class)
public class DbScmCommitPRMapping {
    @JsonProperty("id")
    private final UUID id;

    @JsonProperty("scm_commit_id")
    private final UUID scmCommitId;

    @JsonProperty("scm_pullrequest_id")
    private final UUID scmPullrequestId;

    @JsonProperty("created_at")
    private final Instant createdAt;

}
