package io.levelops.integrations.gitlab.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GitlabStatistics.GitlabStatisticsBuilder.class)
public class GitlabStatistics {
    @JsonProperty("statistics")
    Statistics stats;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Statistics.StatisticsBuilder.class)
    public static class Statistics {
        @JsonProperty("counts")
        Counts counts;

        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = Counts.CountsBuilder.class)
        public static class Counts {
            @JsonProperty("all")
            long all;
            @JsonProperty("closed")
            long closed;
            @JsonProperty("opened")
            long opened;
        }
    }
}
