package io.levelops.commons.databases.models.organization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Sets;
import io.levelops.commons.databases.models.database.organization.DBOrgContentSection;
import io.levelops.commons.models.DefaultListRequest;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = OUConfiguration.OUConfigurationBuilder.class)
public class OUConfiguration {
    @JsonProperty("ou_id")
    UUID ouId;
    @JsonProperty("ou_ref_id")
    Integer ouRefId;
    @JsonProperty("jira_fields")
    @Default
    Set<String> jiraFields = Sets.newHashSet("assignee");
    @JsonProperty("ado_fields")
    @Default
    Set<String> adoFields = Sets.newHashSet("assignee");
    @JsonProperty("github_fields")
    @Default
    Set<String> githubFields = Sets.newHashSet("author");
    @JsonProperty("gitlab_fields")
    @Default
    Set<String> gitlabFields = Sets.newHashSet("author");
    @JsonProperty("bitbucket_fields")
    @Default
    Set<String> bitbucketFields = Sets.newHashSet("author");
    @JsonProperty("bitbucket_server_fields")
    @Default
    Set<String> bitbucketServerFields = Sets.newHashSet("author");
    @JsonProperty("helix_fields")
    @Default
    Set<String> helixFields = Sets.newHashSet("author");
    @JsonProperty("jenkins_fields")
    @Default
    Set<String> jenkinsFields = Sets.newHashSet("user");
    @JsonProperty("azure_pipelines_fields")
    @Default
    Set<String> azurePipelinesFields = Sets.newHashSet("user");
    @JsonProperty("cicd_fields")
    @Default
    Set<String> cicdFields = Sets.newHashSet("user");
    @JsonProperty("scm_fields")
    @Default
    Set<String> scmFields = Sets.newHashSet("author");
    @JsonProperty("pager_duty_fields")
    @Default
    Set<String> pagerDutyFields = Sets.newHashSet("user_id");
    @JsonProperty("sonarqube_fields")
    @Default
    Set<String> sonarqubeFields = Sets.newHashSet("author");

    @JsonProperty("sections")
    @Default
    Set<DBOrgContentSection> sections = Set.of();

    @JsonProperty("request")
    DefaultListRequest request;

    @JsonProperty("ou_exclusions")
    @Default
    List<String> ouExclusions = List.of();

    @JsonProperty("filters")
    @Default
    Map<String, Object> filters = Map.of();

    @JsonProperty("static_users")
    @Default
    Boolean staticUsers = false;
    @JsonProperty("dynamic_users")
    @Default
    Boolean dynamicUsers = false;
    @JsonProperty("integration_ids")
    @Default
    Set<Integer> integrationIds = Set.of();

    public Boolean hasUsersSelection(){
        return staticUsers || dynamicUsers;
    }
}
