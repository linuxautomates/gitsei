package io.levelops.api.model.organization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.harness.authz.acl.model.VaildPermissions;
import lombok.Builder;
import lombok.Value;
import lombok.Builder.Default;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = OrgUnitDTO.OrgUnitDTOBuilder.class)
public class OrgUnitDTO {
    @JsonProperty("id")
    String id;
    @JsonProperty("ou_id")
    UUID ouId;
    @JsonProperty("name")
    String name;
    @JsonProperty("description")
    String description;
    @Default
    @JsonProperty("tags")
    Set<String> tags = Set.of();
    @Default
    @JsonProperty("managers")
    Set<OrgUserId> managers = Set.of();
    @Default
    @JsonProperty("admins")
    Set<OrgUserId> admins = Set.of();
    @Default
    @JsonProperty("sections")
    Set<Section> sections = Set.of();
    @JsonProperty("default_section")
    Section defaultSection;
    @JsonProperty("parent_ref_id")
    Integer parentRefId;
    @JsonProperty("version")
    String version;
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
    @JsonProperty("created_at")
    Instant createdAt;
    @JsonProperty("workflow_profile_id")
    UUID workflowProfileId;
    @JsonProperty("workflow_profile_name")
    String workflowProfileName;
    @JsonProperty("trellis_profile_id")
    UUID trellisProfileId;
    @JsonProperty("trellis_profile_name")
    String trellisProfileName;

    @JsonProperty("access_response")
    VaildPermissions validPermissions;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = OrgUnitDTO.OrgUserId.OrgUserIdBuilder.class)
    public static class OrgUserId {
        @JsonProperty("id")
        String id;
        @JsonProperty("full_name")
        String fullName;
        @JsonProperty("email")
        String email;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = OrgUnitDTO.Section.SectionBuilder.class)
    public static class Section {
        @JsonProperty("id")
        String id;
        @JsonProperty("integrations")
        Map<String, IntegrationDetails> integrations;
        @JsonProperty("dynamic_user_definition")
        Map<String, Object> dynamicUserDefinition;
        @JsonProperty("users")
        Set<String> users;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = OrgUnitDTO.IntegrationDetails.IntegrationDetailsBuilder.class)
    public static class IntegrationDetails {
        @JsonProperty("type")
        String type;
        @JsonProperty("name")
        String name;
        @JsonProperty("filters")
        Map<String, Object> filters;
    }
}
