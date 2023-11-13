package io.levelops.commons.databases.models.database.dashboard;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CloneDashboardRequest {

    @JsonProperty(value = "source_tenant")
    private String sourceTenant;

    @JsonProperty(value = "dashboard_id")
    private String dashboardId;

    @JsonProperty(value = "dashboard_name")
    private String dashboardName;

    @JsonProperty(value = "product_id")
    private String productId;

    @JsonProperty(value = "integration_ids")
    private List<String> integrationIds;

    @JsonProperty(value = "owner_id")
    private String ownerId;

    @JsonProperty(value = "destination_tenant")
    private String destinationTenant;

}
