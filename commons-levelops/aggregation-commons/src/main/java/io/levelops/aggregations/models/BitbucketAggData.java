package io.levelops.aggregations.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.levelops.aggregations.models.bitbucket.BitbucketRepoAggData;
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
@JsonDeserialize(builder = BitbucketAggData.BitbucketAggDataBuilderImpl.class)
public class BitbucketAggData extends AggData {
    @JsonProperty("repo_metrics")
    @Default
    private Map<String, BitbucketRepoAggData> repoMetricsMap = new HashMap<>();

    @JsonPOJOBuilder(withPrefix = "")
    static final class BitbucketAggDataBuilderImpl extends BitbucketAggDataBuilder<BitbucketAggData, BitbucketAggDataBuilderImpl> {
    }
}
