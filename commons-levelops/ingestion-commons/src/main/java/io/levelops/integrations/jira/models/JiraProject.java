package io.levelops.integrations.jira.models;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.Map;
import java.util.Set;

// not using @Value to support @JsonAlias
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JiraProject {

    @JsonProperty("self")
    String self;

    @JsonProperty("id")
    String id;

    @JsonProperty("key")
    String key;

    @JsonProperty("description")
    String description;

    @JsonProperty("lead")
    JiraUser lead;

    @JsonProperty("components")
    List<JiraComponent> components;

    @JsonAlias("issuetypes")
    @JsonProperty("issueTypes")
    List<JiraIssueType> issueTypes;

    @JsonProperty("assigneeType")
    String assigneeType;

    @JsonProperty("versions")
    List<JiraVersion> versions;

    @JsonProperty("name")
    String name;

    @JsonProperty("roles")
    Map<String, String> roles;

    @JsonProperty("projectTypeKey")
    String projectTypeKey;

    @JsonProperty("simplified")
    Boolean simplified;

    @JsonProperty("style")
    String style;

    @JsonProperty("isPrivate")
    Boolean isPrivate;

    @JsonProperty("projectKeys")
    Set<String> projectKeys;

    @JsonProperty("priority_scheme")
    JiraPriorityScheme priorityScheme;

    @JsonProperty("default_priorities")
    List<JiraPriority> defaultPriorities;

    @JsonProperty("ingested_at")
    Long jiraProjectsIngestedAt;

    // region unverified

//     @JsonProperty("properties")
//     JiraProperties properties

//    List<JiraPermission> permissions;

//    @JsonProperty("project_category")
//    JiraProjectCategory projectCategory;

//    @Value
//    @Builder(toBuilder = true)
//    @JsonDeserialize(builder = JiraProjectCategory.JiraProjectCategoryBuilder.class)
//    public static class JiraProjectCategory {
//        @JsonProperty("self")
//        String self;
//        @JsonProperty("id")
//        String id;
//        @JsonProperty("name")
//        String name;
//        @JsonProperty("description")
//        String description;
//    }

    //end region
}
