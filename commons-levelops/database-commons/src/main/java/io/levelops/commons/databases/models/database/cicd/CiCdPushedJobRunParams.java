package io.levelops.commons.databases.models.database.cicd;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CiCdPushedJobRunParams.CiCdPushedJobRunParamsBuilder.class)
public class CiCdPushedJobRunParams {

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("repository")
    String repository;

    @JsonProperty("job_name")
    String jobName;

    @JsonProperty("job_run_number")
    Long jobRunNumber;

    @JsonProperty("params")
    List<CiCdPushedJobRunParam> params;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = CiCdPushedJobRunParam.CiCdPushedJobRunParamBuilder.class)
    public static class CiCdPushedJobRunParam {
        @JsonProperty("type")
        private String type;

        @JsonProperty("name")
        private String name;

        @JsonProperty("value")
        private String value;
    }
}
