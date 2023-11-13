package io.levelops.commons.databases.models.database.temporary;

import io.levelops.integrations.jira.models.JiraIssueFields;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@Builder
@ToString
public class TempJiraIssueFields {
    private String id;
    private JiraIssueFields gif;
    private Long updatedAt;
}
