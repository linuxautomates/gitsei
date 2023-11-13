package io.levelops.commons.databases.models.database.testrails;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.levelops.integrations.testrails.models.Milestone;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbTestRailsMilestone.DbTestRailsMilestoneBuilder.class)
public class DbTestRailsMilestone {

    @JsonProperty("id")
    String id;

    @JsonProperty("milestone_id")
    Integer milestoneId;

    @JsonProperty("project_id")
    Integer projectId;

    @JsonProperty("parent_id")
    Integer parentId;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("name")
    String name;

    @JsonProperty("description")
    String description;

    @JsonProperty("start_on")
    Date startOn;

    @JsonProperty("url")
    String url;

    @JsonProperty("is_started")
    Boolean isStarted;

    @JsonProperty("started_on")
    Date startedOn;

    @JsonProperty("due_on")
    Date dueOn;

    @JsonProperty("is_completed")
    Boolean isCompleted;

    @JsonProperty("completed_on")
    Date completedOn;

    @JsonProperty("refs")
    String refs;

    public static List<DbTestRailsMilestone> fromParentMilestone(Milestone milestone, String integrationId) {
        List<DbTestRailsMilestone> dbTestRailsMilestones = new ArrayList<>();
        dbTestRailsMilestones.add(fromMilestone(milestone, integrationId));
        dbTestRailsMilestones.addAll(getChildMilestones(milestone, integrationId));
        return dbTestRailsMilestones;
    }

    private static List<DbTestRailsMilestone> getChildMilestones(Milestone milestone, String integrationId) {
        List<DbTestRailsMilestone> dbTestRailsMilestones = new ArrayList<>();
        for (Milestone childMilestone : CollectionUtils.emptyIfNull(milestone.getMilestones())) {
            dbTestRailsMilestones.add(fromMilestone(childMilestone, integrationId));
            dbTestRailsMilestones.addAll(getChildMilestones(childMilestone, integrationId));
        }
        return dbTestRailsMilestones;
    }

    private static DbTestRailsMilestone fromMilestone(Milestone milestone, String integrationId) {
        return DbTestRailsMilestone.builder()
                .integrationId(integrationId)
                .milestoneId(milestone.getId())
                .projectId(milestone.getProjectId())
                .parentId(milestone.getParentId())
                .name(milestone.getName())
                .description(StringUtils.truncate(milestone.getDescription(), 100))
                .startOn(milestone.getStartOn())
                .url(milestone.getUrl())
                .isStarted(MoreObjects.firstNonNull(milestone.getIsStarted(), false))
                .startedOn(milestone.getStartedOn())
                .dueOn(milestone.getDueOn())
                .isCompleted(MoreObjects.firstNonNull(milestone.getIsCompleted(), false))
                .completedOn(milestone.getCompletedOn())
                .refs(milestone.getRefs())
                .build();
    }
}
