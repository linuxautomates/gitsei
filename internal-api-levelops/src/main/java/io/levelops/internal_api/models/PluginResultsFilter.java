package io.levelops.internal_api.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.models.DefaultListRequest;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = PluginResultsFilter.PluginResultsFilterBuilder.class)
public class PluginResultsFilter {
    @JsonProperty("ids")
    Set<UUID> ids;

    @JsonProperty("result_ids")
    Set<UUID> resultIds;

    @JsonProperty("versions")
    Set<String> versions;

    @JsonProperty("product_ids")
    Set<String> productIds;

    @JsonProperty("successful")
    Boolean successful;

    @JsonProperty("labels")
    Map<String, List<String>> labels;

    @JsonProperty("tag_ids")
    Set<String> tagIds;

    @Builder.Default
    @JsonProperty("created_at")
    CreatedAtFilter createdAt = CreatedAtFilter.builder().build();

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = CreatedAtFilter.CreatedAtFilterBuilder.class)
    public static class CreatedAtFilter {
        @JsonProperty("from")
        Date from;

        @JsonProperty("to")
        Date to;
    }

    public static PluginResultsFilter fromListRequest(ObjectMapper objectMapper, DefaultListRequest filter) {
        if (filter == null || filter.getFilter() == null) {
            return PluginResultsFilter.builder().build();
        }
        return objectMapper.convertValue(filter.getFilter(), PluginResultsFilter.class);
    }
}