package io.levelops.aggregations.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenericIssueManagementField {
    @JsonProperty
    private String name;

    @JsonProperty
    private String key;

    public static GenericIssueManagementField fromJiraField(DbJiraField jiraField){
        return GenericIssueManagementField.builder()
                .key(jiraField.getFieldKey())
                .name(jiraField.getName())
                .build();
    }

    public static GenericIssueManagementField fromWorkItemField(DbWorkItemField workItemField){
        return GenericIssueManagementField.builder()
                .key(workItemField.getFieldKey())
                .name(workItemField.getName())
                .build();
    }
}
