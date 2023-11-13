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
public class DashboardReport {
    @JsonProperty(value = "id")
    private String id;

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "dashboard_id")
    private String dashboardId;

    @JsonProperty(value = "file_id")
    private String fileId;

    @JsonProperty(value = "created_by")
    private String createdBy;

    @JsonProperty(value = "created_at")
    private Long createdAt;
}
