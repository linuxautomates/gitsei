package io.levelops.commons.databases.models.database.testrails;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.levelops.integrations.testrails.models.Project;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbTestRailsProject.DbTestRailsProjectBuilder.class)
public class DbTestRailsProject {

    private static final int MAX_WIDTH = 100;

    @JsonProperty("id")
    String id;

    @JsonProperty("project_id")
    Integer projectId;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("name")
    String name;

    @JsonProperty("description")
    String description;

    @JsonProperty("url")
    String url;

    @JsonProperty("is_completed")
    Boolean isCompleted;

    @JsonProperty("completed_on")
    Date completedOn;

    @JsonProperty("milestones_list")
    List<DbTestRailsMilestone> milestones;

    public static DbTestRailsProject fromProject(Project project, String integrationId) {
        return DbTestRailsProject.builder()
                .integrationId(integrationId)
                .projectId(project.getId())
                .name(project.getName())
                .description(StringUtils.truncate(project.getAnnouncement(), MAX_WIDTH))
                .url(project.getUrl())
                .isCompleted(MoreObjects.firstNonNull(project.getIsCompleted(), false))
                .completedOn(project.getCompletedOn())
                .milestones(CollectionUtils.emptyIfNull(project.getMilestones()).stream()
                        .flatMap(milestone -> DbTestRailsMilestone.fromParentMilestone(milestone, integrationId).stream())
                        .collect(Collectors.toList()))
                .build();
    }
}
