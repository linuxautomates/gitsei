package io.levelops.aggregations.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.levelops.aggregations.models.gerrit.GerritRepoAggData;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Setter
@Getter
@ToString
@SuperBuilder(toBuilder = true)
@JsonDeserialize(builder = GerritAggData.GerritAggDataBuilderImpl.class)
public class GerritAggData {
    @JsonProperty("repo_metrics")
    @Builder.Default
    private Map<String, GerritRepoAggData> repoMetricsMap = new HashMap<>();

    @JsonPOJOBuilder(withPrefix = "")
    static final class GerritAggDataBuilderImpl extends GerritAggData.GerritAggDataBuilder<GerritAggData, GerritAggData.GerritAggDataBuilderImpl> {
    }
}
