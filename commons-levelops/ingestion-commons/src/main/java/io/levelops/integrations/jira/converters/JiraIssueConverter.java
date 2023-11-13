package io.levelops.integrations.jira.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.integrations.jira.models.JiraComponent;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.jira.models.JiraIssueFields;
import io.levelops.integrations.jira.models.JiraIssueType;
import io.levelops.integrations.jira.models.JiraProject;
import io.levelops.integrations.jira.models.JiraUser;
import io.levelops.integrations.jira.models.JiraVersion;
import io.levelops.integrations.jira.models.NormalizedJiraIssue;
import io.levelops.normalization.Normalizer;
import lombok.extern.log4j.Log4j2;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
public class JiraIssueConverter {

    private static final DateFormat DUE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final String DEFAULT_VALUE = "";

    @Normalizer(contentType = "integration/jira/issues")
    public static NormalizedJiraIssue convert(ObjectMapper objectMapper, JiraIssue jiraIssue) {
        JiraIssueFields fields = jiraIssue.getFields();
        var fieldsOpt = Optional.ofNullable(fields);

        String issueKey = jiraIssue.getKey();

        String projectKey = fieldsOpt.map(JiraIssueFields::getProject).map(JiraProject::getKey).orElse(DEFAULT_VALUE);
        String issueType = fieldsOpt.map(JiraIssueFields::getIssueType).map(JiraIssueType::getName).orElse(DEFAULT_VALUE);
        String issueStatus = fieldsOpt.map(JiraIssueFields::getStatus).map(JiraIssueFields.JiraStatus::getName).orElse(DEFAULT_VALUE);
        String issueAssignee = fieldsOpt.map(JiraIssueFields::getAssignee).map(JiraUser::getDisplayName).orElse(DEFAULT_VALUE);
        String issueAssigneeEmail = fieldsOpt.map(JiraIssueFields::getAssignee).map(JiraUser::getEmailAddress).orElse(DEFAULT_VALUE);
        String issueReporter = fieldsOpt.map(JiraIssueFields::getReporter).map(JiraUser::getDisplayName).orElse(DEFAULT_VALUE);
        String issueReporterEmail = fieldsOpt.map(JiraIssueFields::getReporter).map(JiraUser::getEmailAddress).orElse(DEFAULT_VALUE);
        String issueTitle = fieldsOpt.map(JiraIssueFields::getSummary).orElse(DEFAULT_VALUE);
        String issuePriority = fieldsOpt.map(JiraIssueFields::getPriority).map(JiraIssueFields.JiraPriority::getName).orElse(DEFAULT_VALUE);

        List<String> issueComponents = fieldsOpt.map(JiraIssueFields::getComponents).orElse(Collections.emptyList()).stream()
                .map(JiraComponent::getName)
                .collect(Collectors.toList());

        List<String> issueLabels = fieldsOpt.map(JiraIssueFields::getLabels).orElse(Collections.emptyList());

        List<String> issueFixVersions = fieldsOpt.map(JiraIssueFields::getFixVersions).orElse(Collections.emptyList()).stream()
                .map(JiraVersion::getName)
                .collect(Collectors.toList());

        Date dueDate = fieldsOpt.map(JiraIssueFields::getDueDate).map(dueDateString -> {
            try {
                return Date.from(DUE_DATE_FORMAT.parse(dueDateString).toInstant());
            } catch (ParseException e) {
                log.warn("Failed to parse due date: " + dueDateString, e);
                return null;
            }
        }).orElse(null);

        Optional<Map<String, Object>> dynamicFields = fieldsOpt.map(JiraIssueFields::getDynamicFields);
        return NormalizedJiraIssue.builder()
                .key(issueKey)
                .projectKey(projectKey)
                .type(issueType)
                .status(issueStatus)
                .priority(issuePriority)
                .assignee(issueAssignee)
                .assigneeEmail(issueAssigneeEmail)
                .reporter(issueReporter)
                .reporterEmail(issueReporterEmail)
                .title(issueTitle)
                .components(issueComponents)
                .labels(issueLabels)
                .fixVersions(issueFixVersions)
                .dueAt(dueDate)
                .updatedAt(fields.getUpdated())
                .createdAt(fields.getCreated())
                .customFields(dynamicFields.orElseGet(Map::of))
                .build();
    }

}
