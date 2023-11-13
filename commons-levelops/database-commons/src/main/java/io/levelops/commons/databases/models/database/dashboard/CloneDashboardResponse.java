package io.levelops.commons.databases.models.database.dashboard;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CloneDashboardResponse {

    @JsonProperty(value = "dashboard_id")
    private String dashboardId;

    @JsonProperty(value = "dashboard_name")
    private String dashboardName;

    @JsonProperty(value = "destination_tenant")
    private String destinationTenant;

}
