package io.levelops.commons.dashboard_widget.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DashboardQuery.DashboardQueryBuilder.class)
public class DashboardQuery {
    @JsonProperty("product_id")
    private final String productId;
    @JsonProperty("integration_ids")
    private final List<String> integrationIds;
}