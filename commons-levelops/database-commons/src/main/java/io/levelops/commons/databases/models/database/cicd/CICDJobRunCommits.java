package io.levelops.commons.databases.models.database.cicd;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CICDJobRunCommits.CICDJobRunCommitsBuilder.class)
public class CICDJobRunCommits {
    @JsonProperty("id")
    private final UUID id;

    @JsonProperty("scm_commit_ids")
    private final List<String> scmCommitIds;

    @JsonProperty("cicd_job_id")
    private final UUID cicdJobId;

    @JsonProperty("job_scm_url")
    private final String jobScmUrl;

    @JsonProperty("job_run_updated_at")
    private final Instant jobRunUpdatedAt;
}
