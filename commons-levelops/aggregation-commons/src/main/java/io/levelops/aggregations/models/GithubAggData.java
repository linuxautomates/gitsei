package io.levelops.aggregations.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.levelops.aggregations.models.github.GithubRepoAggData;
import lombok.Builder.Default;
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
@JsonDeserialize(builder = GithubAggData.GithubAggDataBuilderImpl.class)
public class GithubAggData extends AggData {
    @JsonProperty("repo_metrics")
    @Default
    private Map<String, GithubRepoAggData> repoMetricsMap = new HashMap<>();

    @JsonPOJOBuilder(withPrefix = "")
    static final class GithubAggDataBuilderImpl extends GithubAggDataBuilder<GithubAggData, GithubAggDataBuilderImpl> {
    }
}
