package io.levelops.commons.databases.models.database.organization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.harness.authz.acl.model.VaildPermissions;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DBOrgUnit.DBOrgUnitBuilderImpl.class)
public class DBOrgUnit {
    @JsonProperty("id")
    UUID id;
    @JsonProperty("ref_id")
    int refId;
    @JsonProperty("name")
    String name;
    @JsonProperty("description")
    String description;
    @JsonProperty("parent_ref_id")
    Integer parentRefId;
    @JsonProperty("tags")
    Set<String> tags;
    @Default
    @JsonProperty("tag_ids")
    Set<Integer> tagIds = Set.of();
    @JsonProperty("versions")
    Set<Integer> versions;
    @JsonProperty("active")
    boolean active;
    @JsonProperty("created_at")
    Instant createdAt;
    @JsonProperty("updated_at")
    Instant updatedAt;
    @JsonProperty("sections")
    Set<DBOrgContentSection> sections;
    @JsonProperty("managers")
    Set<OrgUserId> managers;
    @JsonProperty("admins")
    Set<PropeloUserId> admins;
    @JsonProperty("no_of_dashboards")
    Integer noOfDashboards;
    @JsonProperty("path")
    String path;
    @JsonProperty("ou_category_id")
    UUID ouCategoryId;
    @JsonProperty("default_dashboard_id")
    Integer defaultDashboardId;
    @JsonProperty("workspace_id")
    Integer workspaceId;
    @JsonProperty("workflow_profile_id")
    UUID workflowProfileId;
    @JsonProperty("workflow_profile_name")
    String workflowProfileName;
    @JsonProperty("trellis_profile_id")
    UUID trellisProfileId;
    @JsonProperty("trellis_profile_name")
    String trellisProfileName;

    @JsonProperty("access_response")
    VaildPermissions vaildPermissions;

    @JsonPOJOBuilder(withPrefix = "")
    static final class DBOrgUnitBuilderImpl extends DBOrgUnitBuilder {
    }
}
