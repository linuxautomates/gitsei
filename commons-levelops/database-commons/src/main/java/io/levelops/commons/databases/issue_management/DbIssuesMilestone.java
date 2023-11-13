package io.levelops.commons.databases.issue_management;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.commons.utils.NumberUtils;
import io.levelops.integrations.azureDevops.models.Iteration;
import io.levelops.integrations.azureDevops.models.Project;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DbIssuesMilestone {

    public static final String PAST_STATE = "past";

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("field_type")
    private String fieldType;

    @JsonProperty(value = "field_value")
    private String fieldValue;

    @JsonProperty(value = "parent_field_value")
    private String parentFieldValue;

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "integration_id")
    private Integer integrationId;

    @JsonProperty(value = "project_id")
    private String projectId;

    @JsonProperty(value = "state")
    private String state;

    @JsonProperty(value = "start_date")
    private Timestamp startDate;

    @JsonProperty(value = "end_date")
    private Timestamp endDate;

    @JsonProperty(value = "completed_at")
    private Timestamp completedAt;

    @JsonProperty(value = "attributes")
    private Map<String, Object> attributes;

    public static DbIssuesMilestone fromAzureDevOpsIteration(String integrationId, Project project, Iteration iteration) {
        return fromAzureDevOpsIteration(integrationId, project, iteration, null, true);
    }

    public static DbIssuesMilestone fromAzureDevOpsIteration(String integrationId, Project project, Iteration iteration, Instant now, boolean closedAfterEndDate) {
        String iterationPath = iteration.getPath();
        String parentFieldValue = null;
        if (Objects.nonNull(iterationPath)) {
            String[] split = iterationPath.split("\\\\");
            parentFieldValue = String.join("\\", Arrays.copyOfRange(split, 0, split.length - 1));
        }
        Timestamp startDate = null;
        Timestamp endDate = null;
        String state = null;
        Iteration.Attributes attributes = iteration.getAttributes();
        if (Objects.nonNull(attributes)) {
            startDate = parseIterationDate(attributes.getStartDate());
            endDate = parseIterationDate(attributes.getFinishDate());
            state = attributes.getTimeFrame();
        }
        if (closedAfterEndDate && endDate != null && now !=null && Timestamp.from(now).after(endDate)) {
            state = PAST_STATE;
        }
        return DbIssuesMilestone.builder()
                .fieldType("sprint")
                .fieldValue(iteration.getId())
                .parentFieldValue(parentFieldValue)
                .name(iteration.getName())
                .integrationId(NumberUtils.toInteger(integrationId))
                .projectId(project.getId())
                .state(state)
                .startDate(startDate)
                .endDate(endDate)
                .completedAt(endDate)
                .attributes(Map.of("project", project.getName()))
                .build();
    }

    @Nullable
    private static Timestamp parseIterationDate(@Nullable String date) {
        if (StringUtils.isEmpty(date)) {
            return null;
        }
        return Timestamp.from(Instant.parse(date).atOffset(ZoneOffset.UTC).toInstant());
    }
}