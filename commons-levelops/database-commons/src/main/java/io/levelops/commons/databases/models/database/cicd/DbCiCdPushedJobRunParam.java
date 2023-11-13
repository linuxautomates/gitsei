package io.levelops.commons.databases.models.database.cicd;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbCiCdPushedJobRunParam.DbCiCdPushedJobRunParamBuilder.class)
public class DbCiCdPushedJobRunParam {

    @JsonProperty("id")
    String id;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("repository")
    String repository;

    @JsonProperty("job_name")
    String jobName;

    @JsonProperty("job_run_number")
    Long jobRunNumber;

    @JsonProperty("job_run_params")
    List<JobRunParam> jobRunParams;

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

    @JsonProperty("created_at")
    Instant createdAt;

    public static DbCiCdPushedJobRunParam fromCiCdPushedJobRunParams(CiCdPushedJobRunParams ciCdPushedJobRunParams) {
        return DbCiCdPushedJobRunParam.builder()
                .integrationId(ciCdPushedJobRunParams.getIntegrationId())
                .repository(ciCdPushedJobRunParams.getRepository())
                .jobName(ciCdPushedJobRunParams.getJobName())
                .jobRunNumber(ciCdPushedJobRunParams.getJobRunNumber())
                .jobRunParams(CollectionUtils.emptyIfNull(ciCdPushedJobRunParams.getParams()).stream().map(param ->
                        JobRunParam.builder()
                                .name(param.getName())
                                .type(param.getType())
                                .value(param.getValue())
                                .build()
                ).collect(Collectors.toList()))
                .build();
    }

}
