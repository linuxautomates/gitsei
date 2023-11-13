package io.levelops.commons.databases.issue_management;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.AzureDevopsCustomFieldConverters;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.utils.NumberUtils;
import io.levelops.integrations.azureDevops.models.Comment;
import io.levelops.integrations.azureDevops.models.Fields;
import io.levelops.integrations.azureDevops.models.Project;
import io.levelops.integrations.azureDevops.models.WorkItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Log4j2
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DbWorkItem {

    public static final String UNASSIGNED = "_UNASSIGNED_";
    public static final String UNPRIORITIZED = "_UNPRIORITIZED_";
    public static final String UNKNOWN = "_UNKNOWN_";

    public static final String VALUE_SEPARATOR = "; ";
    @JsonProperty("id")
    private UUID id;

    @JsonProperty("workitem_id")
    private String workItemId;

    @JsonProperty("integration_id")
    private String integrationId;

    @JsonProperty(value = "summary")
    private String summary;

    @JsonProperty(value = "sprint_ids")
    private List<UUID> sprintIds;

    @JsonProperty(value = "priority")
    private String priority;

    @JsonProperty(value = "assignee")
    private String assignee;

    @JsonProperty(value = "epic")
    private String epic;

    @JsonProperty(value = "parent_workitem_id")
    private String parentWorkItemId;

    @JsonProperty(value = "reporter")
    private String reporter;

    @JsonProperty("assignee_info")
    private DbScmUser assigneeInfo; //contains DbScmUser object for assignee

    @JsonProperty("assignee_id")
    private String assigneeId;

    @JsonProperty("reporter_info")
    private DbScmUser reporterInfo; //contains DbScmUser object for reporter

    @JsonProperty("reporter_id")
    private String reporterId;

    @JsonProperty(value = "status")
    private String status;

    @JsonProperty(value = "workitem_type")
    private String workItemType;

    @JsonProperty(value = "story_point")
    private Float storyPoint;

    @JsonProperty(value = "ingested_at")
    private Long ingestedAt;

    @JsonProperty(value = "custom_fields")
    private Map<String, Object> customFields;

    @JsonProperty(value = "project")
    private String project;

    @JsonProperty("project_id")
    private String projectId;

    @JsonProperty(value = "components")
    private List<String> components;

    @JsonProperty("labels")
    private List<String> labels;

    @JsonProperty(value = "versions")
    private List<String> versions;

    @JsonProperty(value = "fix_versions")
    private List<String> fixVersions;

    @JsonProperty(value = "resolution")
    private String resolution;

    @JsonProperty(value = "status_category")
    private String statusCategory;

    @JsonProperty("ticket_category")
    String ticketCategory;

    @JsonProperty(value = "original_estimate")
    private Float originalEstimate;

    @JsonProperty(value = "desc_size")
    private Integer descSize;

    @JsonProperty(value = "hops")
    private Integer hops;

    @JsonProperty(value = "bounces")
    private Integer bounces;

    @JsonProperty(value = "num_attachments")
    private Integer numAttachments;

    @JsonProperty(value = "workitem_created_at")
    private Timestamp workItemCreatedAt;

    @JsonProperty(value = "workitem_updated_at")
    private Timestamp workItemUpdatedAt;

    @JsonProperty(value = "workitem_resolved_at")
    private Timestamp workItemResolvedAt;

    @JsonProperty(value = "workitem_due_at")
    private Timestamp workItemDueAt;

    @JsonProperty(value = "first_attachment_at")
    private Timestamp firstAttachmentAt;

    @JsonProperty(value = "first_comment_at")
    private Timestamp firstCommentAt;

    @JsonProperty(value = "attributes")
    private Map<String, Object> attributes;

    @JsonProperty(value = "completedWork")
    private Float completedWork;

    @JsonProperty(value = "remainingWork")
    private String remainingWork;

    @JsonProperty(value = "statusChangeDate")
    private Timestamp statusChangeDate;

    @JsonProperty("status_list")
    List<DbWorkItemHistory> statusList;

    @JsonProperty("assignee_list")
    List<DbWorkItemHistory> assigneeList;

    @JsonProperty("is_active")
    @Builder.Default
    private Boolean isActive = true;

    @JsonProperty("response_time")
    Long responseTime;

    @JsonProperty("solve_time")
    Long solveTime;

    @JsonProperty("ticket_portion")
    Double ticketPortion;

    @JsonProperty("story_points_portion")
    Double storyPointsPortion;

    @JsonProperty("assignee_time")
    Long assigneeTime;

    @JsonProperty("sprint_full_names")
    List<String> sprintFullNames;

    public static DbWorkItem fromAzureDevOpsWorkItem(String integrationId, Project project,
                                                     Date fetchTime, WorkItem workItem,
                                                     List<IntegrationConfig.ConfigEntry> customFieldConfig,
                                                     List<DbWorkItemField> customFieldProperties,
                                                     final Map<String, String> statusToStatusCategoryMapping,
                                                     @Nullable String storyPointsField) {
        Fields fields = workItem.getFields() != null ? workItem.getFields() : Fields.builder().build();
        Timestamp createdDate = parseDate(fields.getCreatedDate());
        Timestamp updatedDate = parseDate(fields.getUpdatedDate());
        Timestamp resolvedDate = parseDate(fields.getResolvedDate());
        Timestamp closedDate = parseDate(fields.getClosedDate());
        Timestamp statusChangeDate = parseDate(fields.getStateChangeDate());
        Map<String, Object> customFields = parseCustomFields(workItem, customFieldConfig, customFieldProperties);
        float storyPoints = parseStoryPoints(fields, storyPointsField);
        String projectName = Optional.of(project.getName()).orElse("");
        String orgName = Optional.of(project.getOrganization()).orElse("");
        return DbWorkItem.builder()
                .workItemId(workItem.getId())
                .integrationId(integrationId)
                .summary(fields.getTitle())
                .descSize(StringUtils.isNotEmpty(workItem.getFields().getDescription()) ? workItem.getFields().getDescription().length() : 0)
//                .sprintIds(workItem.getFields().getIterationId() != null ?
//                        Arrays.asList(UUID.fromString(workItem.getFields().getIterationId().toString())) : List.of())
                .priority(MoreObjects.firstNonNull(fields.getPriority(), UNPRIORITIZED).toString())
                .assignee(fields.getAssignedTo() != null ? fields.getAssignedTo().getDisplayName() : UNASSIGNED)
                .assigneeInfo(DbScmUser.fromADWorkItemsAssignee(workItem, integrationId))
                .parentWorkItemId(fields.getParent() != null ? String.valueOf(fields.getParent()) : null)
                .reporter(fields.getCreatedBy().getUniqueName())
                .reporterInfo(DbScmUser.fromADWorkItemsReporter(workItem, integrationId))
                .status(fields.getState())
                .epic(UNKNOWN)
                .resolution(UNKNOWN)
                .sprintIds(List.of())
                .workItemType(MoreObjects.firstNonNull(fields.getWorkItemType(), UNKNOWN))
                .storyPoint(storyPoints)
                .project(orgName + "/" + projectName)
                .projectId(project.getId())
                .labels(fields.getTags() != null ? Arrays.asList(fields.getTags().split(VALUE_SEPARATOR)) : List.of())
                .versions(List.of(UNKNOWN))
                .fixVersions(List.of(UNKNOWN))
                .statusCategory(MapUtils.emptyIfNull(statusToStatusCategoryMapping).getOrDefault(fields.getState(), fields.getState()))
                .workItemCreatedAt(createdDate)
                .workItemUpdatedAt(updatedDate)
                .firstAttachmentAt(extractFirstAttachmentCreatedAtDate(workItem))
                .firstCommentAt(getFirstCommentAt(workItem))
                .workItemResolvedAt(ObjectUtils.firstNonNull(resolvedDate, closedDate)) // LEV-3873 some customers don't seem to have ResolvedDate
                .attributes(getAttributes(project, fields))
                .ingestedAt(DateUtils.truncate(fetchTime, Calendar.DATE))
                .customFields(customFields)
                .completedWork(Float.valueOf(Optional.ofNullable(fields.getCompletedWork()).orElse("0")))
                .remainingWork(MoreObjects.firstNonNull(fields.getRemainingWork(), "0"))
                .originalEstimate(Float.valueOf(MoreObjects.firstNonNull(fields.getOriginalEstimate(), "0")))
                .statusChangeDate(statusChangeDate)
                .isActive(true)
                .build();
    }

    protected static float parseStoryPoints(Fields fields, @Nullable String storyPointsField) {
        if (storyPointsField == null) {
            return MoreObjects.firstNonNull(fields.getStoryPoints(), 0f);
        }
        Float output = null;
        if ("effort".equalsIgnoreCase(storyPointsField) && fields.getEffort() != null) {
            output = fields.getEffort().floatValue();
        } else if (fields.getCustomFields().get(storyPointsField) != null) {
            Object storyPoints = fields.getCustomFields().get(storyPointsField);
            if (storyPoints instanceof Number) {
                output = (Float) storyPoints;
            } else if (storyPoints instanceof String) {
                output = NumberUtils.toFloat((String) storyPoints, 0f);
            }
        }
        return MoreObjects.firstNonNull(output, 0f);
    }

    @Nullable
    protected static Timestamp parseDate(@Nullable String date) {
        return Objects.nonNull(date) ? Timestamp.from(DateUtils.parseDateTime(date)) : null;
    }

    @NotNull
    private static Map<String, Object> getAttributes(Project project, Fields fields) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("organization", project.getOrganization());
        attributes.put("project", project.getName());
        if (fields.getAreaPath() != null) {
            attributes.put("code_area", fields.getAreaPath());
        }
        if (fields.getAcceptanceCriteria() != null) {
            attributes.put("acceptance_criteria", StringUtils.truncate(fields.getAcceptanceCriteria(), 50));
        }
        if (fields.getEffort() != null) {
            attributes.put("effort", fields.getEffort());
        }
        return attributes;
    }

    @Nullable
    protected static Timestamp getFirstCommentAt(WorkItem workItem) {
        return IterableUtils.getFirst(workItem.getComments())
                .map(Comment::getCreatedDate)
                .map(DbWorkItem::parseDate)
                .orElse(null);
    }

    public static Map<String, Object> parseCustomFields(WorkItem workItem,
                                                        List<IntegrationConfig.ConfigEntry> customFieldConfig,
                                                        List<DbWorkItemField> customFieldProperties) {
        if (workItem == null || workItem.getFields() == null || CollectionUtils.isEmpty(customFieldConfig)) {
            return Map.of();
        }

        Fields workItemFields = workItem.getFields();
        Map<String, Object> workItemCustomFields = MapUtils.emptyIfNull(workItemFields.getCustomFields());
        // -- special handling of those fields because they are modelled in the POJO but not used as standard fields
        if (workItemFields.getEffort() != null) {
            workItemCustomFields.put("Microsoft.VSTS.Scheduling.Effort", workItemFields.getEffort());
        }
        if (workItemFields.getValueArea() != null) {
            workItemCustomFields.put("Microsoft.VSTS.Common.ValueArea", workItemFields.getValueArea());
        }

        Map<String, Object> customFields = new HashMap<>();
        for (IntegrationConfig.ConfigEntry entry : customFieldConfig) {
            if (entry.getKey() == null) {
                continue;
            }
            Object value = workItemCustomFields.get(entry.getKey());
            log.debug("workItemId={} customField={} value={}", workItem.getId(), entry.getKey(), value);
            String fieldType = customFieldProperties
                    .stream()
                    .filter(c -> c.getFieldKey().equalsIgnoreCase(entry.getKey()))
                    .map(DbWorkItemField::getFieldType)
                    .findFirst()
                    .orElse(null);
            Object parsedValue = AzureDevopsCustomFieldConverters.parseValue(value, fieldType);
            if (parsedValue != null) {
                customFields.put(entry.getKey(), parsedValue);
            }
        }
        return customFields;
    }

    private static Timestamp extractFirstAttachmentCreatedAtDate(WorkItem workItem) {
        if (CollectionUtils.isEmpty(workItem.getRelations())) {
            return null;
        }
        WorkItem.WorkItemRelation lastWorkItemRelation = workItem.getRelations().get(workItem.getRelations().size() - 1);
        if (lastWorkItemRelation.getAttributes() == null || lastWorkItemRelation.getAttributes().getResourceCreatedDate() == null) {
            return null;
        }
        Instant createdAt = DateUtils.parseDateTime(lastWorkItemRelation.getAttributes().getResourceCreatedDate());
        if (createdAt == null) {
            return null;
        }
        return Timestamp.from(createdAt);
    }
}
