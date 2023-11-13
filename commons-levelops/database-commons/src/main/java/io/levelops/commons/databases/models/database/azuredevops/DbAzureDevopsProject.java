package io.levelops.commons.databases.models.database.azuredevops;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.CaseFormat;
import io.levelops.integrations.azureDevops.models.EnrichedProjectData;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbAzureDevopsProject.DbAzureDevopsProjectBuilder.class)
public class DbAzureDevopsProject {

    @JsonProperty("organization")
    String organization;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("id")
    String id;

    @JsonProperty("product_id")
    String productId;

    @JsonProperty("name")
    String name;

    @JsonProperty("state")
    String state;

    @JsonProperty("revision")
    int revision;

    @JsonProperty("visibility")
    String visibility;

    @JsonProperty("lastUpdateTime")
    String lastUpdateTime;

    @JsonProperty("description")
    String description;

    @JsonProperty("pipelines")
    List<DbAzureDevopsPipelineRun> pipelineRuns;

    @JsonProperty("releases")
    List<DbAzureDevopsRelease> releases;

    @JsonProperty("builds")
    List<DbAzureDevopsBuild> builds;

    public static DbAzureDevopsProject fromPipelineRuns(EnrichedProjectData enrichedProjectData, String integrationId,
                                                        String productId, Date ingestedAt) {
        return DbAzureDevopsProject.builder()
                .organization(enrichedProjectData.getProject().getOrganization())
                .integrationId(integrationId)
                .id(enrichedProjectData.getProject().getId())
                .productId(productId)
                .name(enrichedProjectData.getProject().getName())
                .state(camelCaseToSnakeCase(enrichedProjectData.getProject().getState()))
                .revision(enrichedProjectData.getProject().getRevision())
                .visibility(camelCaseToSnakeCase(enrichedProjectData.getProject().getVisibility()))
                .lastUpdateTime(enrichedProjectData.getProject().getLastUpdateTime())
                .description(enrichedProjectData.getProject().getDescription())
                .pipelineRuns(CollectionUtils.emptyIfNull(enrichedProjectData.getPipelineRuns()).stream()
                        .map(run -> DbAzureDevopsPipelineRun.fromRun(enrichedProjectData.getPipeline(), run, ingestedAt))
                        .collect(Collectors.toList()))
                .build();
    }

    public static DbAzureDevopsProject fromReleases(EnrichedProjectData enrichedProjectData, String integrationId,
                                                        String productId, Date ingestedAt) {
        return DbAzureDevopsProject.builder()
                .organization(enrichedProjectData.getProject().getOrganization())
                .integrationId(integrationId)
                .id(enrichedProjectData.getProject().getId())
                .productId(productId)
                .name(enrichedProjectData.getProject().getName())
                .state(camelCaseToSnakeCase(enrichedProjectData.getProject().getState()))
                .revision(enrichedProjectData.getProject().getRevision())
                .visibility(camelCaseToSnakeCase(enrichedProjectData.getProject().getVisibility()))
                .lastUpdateTime(enrichedProjectData.getProject().getLastUpdateTime())
                .description(enrichedProjectData.getProject().getDescription())
                .releases(CollectionUtils.emptyIfNull(enrichedProjectData.getReleases()).stream()
                        .map(release -> DbAzureDevopsRelease.fromRelease(release, ingestedAt))
                        .collect(Collectors.toList()))
                .build();
    }

    public static String camelCaseToSnakeCase(String camelCase) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, camelCase);
    }
}
