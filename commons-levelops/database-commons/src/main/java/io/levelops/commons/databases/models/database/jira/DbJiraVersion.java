package io.levelops.commons.databases.models.database.jira;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.utils.NumberUtils;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.jira.models.JiraProject;
import io.levelops.integrations.jira.models.JiraVersion;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbJiraVersion.DbJiraVersionBuilder.class)
public class DbJiraVersion {

    @JsonProperty("id")
    String id; // internal id

    @JsonProperty("version_id")
    Integer versionId;

    @JsonProperty("project_id")
    Integer projectId;

    @JsonProperty("project_key")
    String projectKey;

    @JsonProperty("name")
    String name;

    @JsonProperty("description")
    String description;

    @JsonProperty("integration_id")
    Integer integrationId;

    @JsonProperty("archived")
    Boolean archived;

    @JsonProperty("released")
    Boolean released;

    @JsonProperty("overdue")
    Boolean overdue;

    @JsonProperty("start_date")
    Instant startDate;

    @JsonProperty("end_date")
    Instant endDate;
    
    @JsonProperty("fix_version_updated_at")
    Long fixVersionUpdatedAt;

    public static List<DbJiraVersion> fromJiraIssue(JiraIssue source, String integrationId) {
        if (Objects.isNull(source) ||
                Objects.isNull(source.getFields()) ||
                (CollectionUtils.isEmpty(source.getFields().getVersions()) &&
                        CollectionUtils.isEmpty(source.getFields().getFixVersions()))) {
            return List.of();
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        List<JiraVersion> versions = Optional.ofNullable(source.getFields().getVersions()).orElse(List.of());
        List<JiraVersion> fixVersions = Optional.ofNullable(source.getFields().getFixVersions()).orElse(List.of());
        return Stream.concat(versions.stream(), fixVersions.stream())
                .filter(Objects::nonNull)
                .map(version -> {
                    Instant startInstant = null;
                    Instant endInstant = null;
                    try {
                        startInstant = version.getStartDate() != null
                                ? simpleDateFormat.parse(version.getStartDate()).toInstant()
                                : null;
                        endInstant = version.getReleaseDate() != null
                                ? simpleDateFormat.parse(version.getReleaseDate()).toInstant()
                                : null;
                    } catch (ParseException e) {
                        log.error("Unable to parse date string", e);
                    }
                    return DbJiraVersion.builder()
                            .versionId(NumberUtils.toInteger(version.getId()))
                            .projectId(NumberUtils.toInteger(version.getProjectId()))
                            .name(version.getName())
                            .description(version.getDescription())
                            .integrationId(NumberUtils.toInteger(integrationId))
                            .archived(version.getArchived())
                            .released(version.getReleased())
                            .overdue(version.getOverdue())
                            .startDate(startInstant)
                            .endDate(endInstant)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public static List<DbJiraVersion> fromJiraProject(DbJiraProject project, String integrationId) {
        if (Objects.isNull(project) ||
                Objects.isNull(project.getVersions())) {
            return List.of();
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        List<JiraVersion> fixVersions = Optional.ofNullable(project.getVersions()).orElse(List.of());
        Long jiraVersionsUpdatedAt = project.getJiraProjectsIngestedAt();
        return fixVersions.stream()
                .filter(Objects::nonNull)
                .map(version -> {
                    Instant startInstant = null;
                    Instant endInstant = null;
                    try {
                        startInstant = version.getStartDate() != null
                                ? simpleDateFormat.parse(version.getStartDate()).toInstant()
                                : null;
                    } catch (ParseException e) {
                        log.error("Unable to parse start date string", e);
                    }
                    try {
                        endInstant = version.getReleaseDate() != null
                                ? simpleDateFormat.parse(version.getReleaseDate()).toInstant()
                                : null;
                    } catch (ParseException e) {
                        log.error("Unable to parse release date string", e);
                    }
                    return DbJiraVersion.builder()
                            .versionId(NumberUtils.toInteger(version.getId()))
                            .projectId(NumberUtils.toInteger(version.getProjectId()))
                            .name(version.getName())
                            .description(version.getDescription())
                            .integrationId(NumberUtils.toInteger(integrationId))
                            .archived(version.getArchived())
                            .released(version.getReleased())
                            .overdue(version.getOverdue())
                            .startDate(startInstant)
                            .endDate(endInstant)
                            .fixVersionUpdatedAt((jiraVersionsUpdatedAt != null) ? jiraVersionsUpdatedAt : 0L)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
