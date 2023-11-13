package io.levelops.commons.databases.models.database.azuredevops;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.dates.DateUtils;
import io.levelops.integrations.azureDevops.models.AzureDevopsRelease;
import io.levelops.integrations.azureDevops.models.AzureDevopsReleaseDefinition;
import io.levelops.integrations.azureDevops.models.Configuration;
import io.levelops.integrations.azureDevops.models.IdentityRef;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbAzureDevopsRelease.DbAzureDevopsReleaseBuilder.class)
public class DbAzureDevopsRelease {
    @JsonProperty("id")
    String id;
    @JsonProperty("release_id")
    Integer releaseId;
    @JsonProperty("name")
    String name;
    @JsonProperty("definition")
    AzureDevopsReleaseDefinition definition;
    @JsonProperty("status")
    String status;
    @JsonProperty("createdOn")
    Instant createdOn;
    @JsonProperty("modifiedOn")
    Instant modifiedOn;
    @JsonProperty("start_time")
    Instant startTime;
    @JsonProperty("finish_time")
    Instant finishTime;
    @JsonProperty("createdBy")
    String createdBy;
    @JsonProperty("variables")
    Map<String, Configuration.Variable> variables;
    @JsonProperty("metadata")
    Map<String, Object> metadata;
    @JsonProperty("ingested_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:SSZ")
    Date ingestedAt;
    @JsonProperty("stages")
    List<DbAzureDevopsReleaseStage> stages;

    public static DbAzureDevopsRelease fromRelease(AzureDevopsRelease release, Date ingestedAt) {
        return DbAzureDevopsRelease.builder()
                .releaseId(release.getId())
                .name(release.getName())
                .definition(release.getDefinition())
                .status(release.getStatus())
                .createdOn(DateUtils.parseDateTime(release.getCreatedOn()))
                .modifiedOn(DateUtils.parseDateTime(release.getModifiedOn()))
                .startTime(DateUtils.parseDateTime(release.getStartTime()))
                .finishTime(DateUtils.parseDateTime(release.getFinishTime()))
                .createdBy(Optional.ofNullable(release.getCreatedBy()).map(IdentityRef::getUniqueName).orElse(null))
                .variables(getVariables(release))
                .stages(ListUtils.emptyIfNull(release.getStages()).stream().map(DbAzureDevopsReleaseStage::fromEnvironment).collect(Collectors.toList()))
                .ingestedAt(ingestedAt)
                .metadata(new HashMap<>(Map.of("tags", release.getTags(), "artifacts", release.getArtifacts())))
                .build();
    }
    public static Map<String, Configuration.Variable> getVariables(AzureDevopsRelease release) {
        Map<String, Configuration.Variable> variables = new HashMap<>();
        ListUtils.emptyIfNull(release.getVariableGroups()).stream()
                .map(Configuration.VariableGroup::getVariables)
                .filter(MapUtils::isNotEmpty)
                .forEach(variables::putAll);
        if (MapUtils.isNotEmpty(release.getVariables())) {
            variables.putAll(release.getVariables());
        }
        return variables;
    }
}
