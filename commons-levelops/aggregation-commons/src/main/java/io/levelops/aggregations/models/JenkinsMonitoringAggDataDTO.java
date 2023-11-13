package io.levelops.aggregations.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.levelops.commons.databases.models.database.AggregationRecord;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@SuperBuilder(toBuilder = true)
@JsonDeserialize(builder = JenkinsMonitoringAggDataDTO.JenkinsMonitoringAggDataDTOBuilderImpl.class)
public class JenkinsMonitoringAggDataDTO extends JenkinsMonitoringAggData {
    @JsonProperty("id")
    private String id;

    @JsonProperty("type")
    private AggregationRecord.Type type;

    @JsonProperty("tool_type")
    private String toolType;

    @JsonProperty("product_ids")
    private List<Integer> productIds;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonPOJOBuilder(withPrefix = "")
    static final class JenkinsMonitoringAggDataDTOBuilderImpl extends JenkinsMonitoringAggDataDTO.JenkinsMonitoringAggDataDTOBuilder<JenkinsMonitoringAggDataDTO, JenkinsMonitoringAggDataDTO.JenkinsMonitoringAggDataDTOBuilderImpl> {
    }
}
