package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CICDJobRun.CICDJobRunBuilder.class)
public class CICDJobRun {
    @JsonProperty("id")
    UUID id;
    @JsonProperty("cicd_job_id")
    UUID cicdJobId;
    @JsonProperty("job_run_number")
    Long jobRunNumber;
    @JsonProperty("status")
    String status;
    @JsonProperty("start_time")
    Instant startTime;
    @JsonProperty("duration")
    Integer duration;
    @JsonProperty("end_time")
    Instant endTime;
    @JsonProperty("cicd_user_id")
    String cicdUserId;
    @JsonProperty("scm_commit_ids")
    List<String> scmCommitIds;
    @JsonProperty("source")
    Source source;
    @JsonProperty("reference_id")
    String referenceId;
    @JsonProperty("log_gcspath")
    String logGcspath;
    @JsonProperty("params")
    List<JobRunParam> params;
    @JsonProperty("triggers")
    Set<CICDJobTrigger> triggers;
    @JsonProperty("ci")
    Boolean ci;
    @JsonProperty("cd")
    Boolean cd;
    @JsonProperty("metadata")
    Map<String, Object> metadata;
    @JsonProperty("created_at")
    private Instant createdAt;
    @JsonProperty("updated_at")
    private Instant updatedAt;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = JobRunParam.JobRunParamBuilder.class)
    public static class JobRunParam {
        @JsonProperty("type")
        private String type;
        @JsonProperty("name")
        private String name;
        @JsonProperty("value")
        private String value;
    }


    public enum Source {
        JOB_RUN_COMPLETE_EVENT,
        ANALYTICS_PERIODIC_PUSH;

        @JsonCreator
        @Nullable
        public static Source fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(Source.class, value);
        }

        @JsonValue
        @Override
        public String toString() {
            return super.toString();
        }
    }
}
