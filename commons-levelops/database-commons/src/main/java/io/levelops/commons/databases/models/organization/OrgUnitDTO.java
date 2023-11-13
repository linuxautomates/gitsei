package io.levelops.commons.databases.models.organization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

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
    @JsonProperty("sections")
    Set<Section> sections = Set.of();
    @JsonProperty("default_section")
    Section defaultSection;
    @JsonProperty("parent_ref_id")
    Integer parentRefId;
    @JsonProperty("version")
    String version;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = OrgUserId.OrgUserIdBuilder.class)
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
    @JsonDeserialize(builder = Section.SectionBuilder.class)
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
    @JsonDeserialize(builder = IntegrationDetails.IntegrationDetailsBuilder.class)
    public static class IntegrationDetails {
        @JsonProperty("type")
        String type;
        @JsonProperty("name")
        String name;
        @JsonProperty("filters")
        Map<String, Object> filters;
    }
}
