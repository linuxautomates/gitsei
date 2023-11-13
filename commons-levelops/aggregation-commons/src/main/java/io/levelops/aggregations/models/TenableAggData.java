package io.levelops.aggregations.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.levelops.commons.databases.models.database.temporary.TempTenableVulnObject;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Setter
@Getter
@ToString
@SuperBuilder(toBuilder = true)
@JsonDeserialize(builder = TenableAggData.TenableAggDataBuilderImpl.class)
public class TenableAggData extends AggData {
    @JsonProperty("agg_by_severity")
    @Builder.Default
    private Map<String, List<TempTenableVulnObject>> aggBySeverity = new HashMap<>();

    @JsonProperty("agg_by_status")
    @Builder.Default
    private Map<String, List<TempTenableVulnObject>> aggByStatus = new HashMap<>();

    @JsonProperty("agg_by_status_time_series")
    @Builder.Default
    private Map<Long, Map<String, Long>> aggByStatusTimeSeries = new HashMap<>();

    @JsonPOJOBuilder(withPrefix = "")
    static final class TenableAggDataBuilderImpl extends TenableAggData.TenableAggDataBuilder<TenableAggData, TenableAggData.TenableAggDataBuilderImpl> {
    }
}
