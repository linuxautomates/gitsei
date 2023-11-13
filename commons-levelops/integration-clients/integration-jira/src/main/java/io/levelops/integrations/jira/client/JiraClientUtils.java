package io.levelops.integrations.jira.client;

import io.levelops.integrations.jira.utils.JiraUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.jira.models.JiraApiSearchResult;
import io.levelops.integrations.jira.models.JiraComment;
import io.levelops.integrations.jira.models.JiraCommentsResult;
import io.levelops.integrations.jira.models.JiraCreateMeta;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.jira.models.JiraIssueChangeLog;
import io.levelops.integrations.jira.models.JiraIssueFields;
import io.levelops.integrations.jira.models.JiraMyself;
import io.levelops.integrations.jira.models.JiraProject;
import io.levelops.integrations.jira.models.JiraUser;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Jira ClientUtils class which should be used for removing sensitive fields from the fetched jira issues
 */
public class JiraClientUtils {

    /**
     * @param jiraApiSearchResult the fetched jira issues along with change logs from jira client
     * @param sensitiveFields     the fields need to be removed from each jira issues and change logs
     * @return {@link JiraApiSearchResult} containing the removed sensitive fields from each jira issues and change logs
     */
    public static JiraApiSearchResult sanitizeResults(JiraApiSearchResult jiraApiSearchResult, List<String> sensitiveFields) {
        if (CollectionUtils.isEmpty(sensitiveFields)) {
            return jiraApiSearchResult;
        }
        return jiraApiSearchResult.toBuilder().issues(
                        jiraApiSearchResult.getIssues()
                                .stream()
                                .map(jiraIssue -> sanitizeEachJiraIssue(jiraIssue, sensitiveFields))
                                .collect(Collectors.toList()))
                .build();
    }

    private static JiraIssue sanitizeEachJiraIssue(JiraIssue jiraIssue, List<String> sensitiveFields) {
        if (jiraIssue == null) {
            return null;
        }
        return jiraIssue.toBuilder()
                .firstCommentAt(JiraUtils.extractFirstCommentTimeFromJiraIssue(jiraIssue))
                .fields(removeSensitiveFields(jiraIssue.getFields(), sensitiveFields))
                .changeLog(removeSensitiveFields(jiraIssue.getChangeLog(), sensitiveFields))
                .build();
    }

    public static JiraCommentsResult sanitizeJiraCommentsResult(JiraCommentsResult jiraCommentsResult,
                                                                List<String> sensitiveFields) {
        if (CollectionUtils.isEmpty(sensitiveFields) || jiraCommentsResult == null || jiraCommentsResult.getComments() == null) {
            return jiraCommentsResult;
        }
        return jiraCommentsResult.toBuilder()
                .comments(sensitiveFields.stream().anyMatch("comment"::equalsIgnoreCase) ? null
                        : sanitizeJiraComments(jiraCommentsResult.getComments(), sensitiveFields))
                .build();
    }

    private static List<JiraComment> sanitizeJiraComments(List<JiraComment> jiraComments, List<String> sensitiveFields) {
        return jiraComments.stream()
                .map(jiraComment -> jiraComment.toBuilder()
                        .author(sanitizeJiraUser(jiraComment.getAuthor(), sensitiveFields))
                        .updateAuthor(sanitizeJiraUser(jiraComment.getUpdateAuthor(), sensitiveFields))
                        .build())
                .collect(Collectors.toList());
    }

    public static JiraUser sanitizeJiraUser(JiraUser jiraUser, List<String> sensitiveFields) {
        if (CollectionUtils.isEmpty(sensitiveFields) || jiraUser == null) {
            return jiraUser;
        }
        boolean excludeEmailAddress = sensitiveFields.stream()
                .anyMatch(field -> field.equalsIgnoreCase("emailAddress"));
        if (excludeEmailAddress) {
            return jiraUser.toBuilder().emailAddress(null).build();
        }
        return jiraUser;
    }

    public static List<JiraUser> sanitizeJiraUsers(List<JiraUser> jiraUsers, List<String> sensitiveFields) {
        if (CollectionUtils.isEmpty(sensitiveFields) || jiraUsers == null) {
            return jiraUsers;
        }
        return jiraUsers.stream()
                .map(jiraUser -> JiraClientUtils.sanitizeJiraUser(jiraUser, sensitiveFields))
                .collect(Collectors.toList());
    }

    public static JiraProject sanitizeJiraProject(JiraProject jiraProject, List<String> sensitiveFields) {
        if (CollectionUtils.isEmpty(sensitiveFields) || jiraProject == null || jiraProject.getLead() == null) {
            return jiraProject;
        }
        return jiraProject.toBuilder()
                .lead(sanitizeJiraUser(jiraProject.getLead(), sensitiveFields))
                .build();
    }

    public static List<JiraProject> sanitizeJiraProjects(List<JiraProject> jiraProjects, List<String> sensitiveFields) {
        if (CollectionUtils.isEmpty(sensitiveFields) || jiraProjects == null) {
            return jiraProjects;
        }
        return jiraProjects.stream()
                .map(jiraProject -> sanitizeJiraProject(jiraProject, sensitiveFields))
                .collect(Collectors.toList());
    }

    public static JiraMyself sanitizeJiraMyself(JiraMyself jiraMyself, List<String> sensitiveFields) {
        if (CollectionUtils.isEmpty(sensitiveFields) || jiraMyself == null) {
            return jiraMyself;
        }
        boolean excludeEmailAddress = sensitiveFields.stream()
                .anyMatch(field -> field.equalsIgnoreCase("emailAddress"));
        if (excludeEmailAddress) {
            return jiraMyself.toBuilder().emailAddress(null).build();
        }
        return jiraMyself;
    }

    public static JiraCreateMeta sanitizeJiraCreateMeta(JiraCreateMeta jiraCreateMeta, List<String> sensitiveFields) {
        if (CollectionUtils.isEmpty(sensitiveFields) || jiraCreateMeta == null || jiraCreateMeta.getProjects() == null) {
            return jiraCreateMeta;
        }
        return jiraCreateMeta.toBuilder()
                .projects(sanitizeJiraProjects(jiraCreateMeta.getProjects(), sensitiveFields))
                .build();
    }

    private static JiraIssueFields removeSensitiveFields(JiraIssueFields fields, List<String> sensitiveFields) {
        JiraIssueFields.JiraIssueFieldsBuilder jiraIssueFieldsBuilder = fields.toBuilder();
        Map<String, Object> dynamicFields = fields.getDynamicFields();
        sensitiveFields.forEach(sensitiveField -> {
            if (sensitiveField.equalsIgnoreCase("Summary")) {
                jiraIssueFieldsBuilder.summary(null);
            }
            if (sensitiveField.equalsIgnoreCase("Description")) {
                jiraIssueFieldsBuilder.description(null);
            }
            dynamicFields.remove(sensitiveField);
        });
        jiraIssueFieldsBuilder.assignee(sanitizeJiraUser(fields.getAssignee(), sensitiveFields));
        jiraIssueFieldsBuilder.creator(sanitizeJiraUser(fields.getCreator(), sensitiveFields));
        jiraIssueFieldsBuilder.reporter(sanitizeJiraUser(fields.getReporter(), sensitiveFields));
        jiraIssueFieldsBuilder.project(sanitizeJiraProject(fields.getProject(), sensitiveFields));
        jiraIssueFieldsBuilder.comment(sanitizeJiraCommentsResult(fields.getComment(), sensitiveFields));
        if (CollectionUtils.isNotEmpty(fields.getIssueLinks())) {
            jiraIssueFieldsBuilder.issueLinks(removeSensitiveFieldsFromIssueLink(fields.getIssueLinks(), sensitiveFields));
        }
        if (CollectionUtils.isNotEmpty(fields.getSubtasks())) {
            jiraIssueFieldsBuilder.subtasks(fields.getSubtasks()
                    .stream()
                    .map(subTask -> sanitizeEachJiraIssue(subTask, sensitiveFields))
                    .collect(Collectors.toList()));
        }
        JiraIssueFields newJiraIssueFields = jiraIssueFieldsBuilder.build();
        for (Map.Entry<String, Object> dynamicField : dynamicFields.entrySet()) {
            if (dynamicField.getKey().equalsIgnoreCase("parent")) {
                JiraIssue parentJiraIssue = DefaultObjectMapper.get()
                        .convertValue(dynamicField.getValue(), JiraIssue.class);
                JiraIssue jiraIssue = sanitizeEachJiraIssue(parentJiraIssue, sensitiveFields);
                newJiraIssueFields.addDynamicField(dynamicField.getKey(), jiraIssue);
            } else {
                Optional<String> emailAddress = sensitiveFields.stream()
                        .filter(field -> field.equalsIgnoreCase("emailAddress"))
                        .findFirst();
                if (emailAddress.isPresent()
                        && (dynamicField.getValue() instanceof Map || dynamicField.getValue() instanceof List)) {
                    removeSensitiveFieldFromObject(dynamicField.getValue(), emailAddress.get());
                }
                newJiraIssueFields.addDynamicField(dynamicField.getKey(), dynamicField.getValue());
            }
        }
        return newJiraIssueFields;
    }

    @SuppressWarnings("unchecked")
    private static void removeSensitiveFieldFromObject(Object object, String sensitiveField) {
        if (object instanceof Map) {
            removeSensitiveFieldFromMap((Map<String, Object>) object, sensitiveField);
        } else if (object instanceof List) {
            removeSensitiveFieldFromList((List<Object>) object, sensitiveField);
        }
    }

    private static void removeSensitiveFieldFromMap(Map<String, Object> map, String sensitiveField) {
        if (MapUtils.isEmpty(map)) {
            return;
        }
        if (map.containsKey(sensitiveField)) {
            map.put(sensitiveField, null);
        }
        for (Object value : map.values()) {
            removeSensitiveFieldFromObject(value, sensitiveField);
        }
    }

    private static void removeSensitiveFieldFromList(List<Object> list, String sensitiveField) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        for (Object value : list) {
            removeSensitiveFieldFromObject(value, sensitiveField);
        }
    }

    private static List<JiraIssueFields.JiraIssueLink> removeSensitiveFieldsFromIssueLink(List<JiraIssueFields.JiraIssueLink> issueLinks,
                                                                                          List<String> sensitiveFields) {
        if (CollectionUtils.isEmpty(issueLinks)) {
            return Collections.emptyList();
        }
        return issueLinks.stream().map(issueLink -> issueLink.toBuilder()
                        .inwardIssue(sanitizeEachJiraIssue(issueLink.getInwardIssue(), sensitiveFields))
                        .outwardIssue(sanitizeEachJiraIssue(issueLink.getOutwardIssue(), sensitiveFields))
                        .build())
                .collect(Collectors.toList());
    }

    private static JiraIssueChangeLog removeSensitiveFields(JiraIssueChangeLog changeLog, List<String> sensitiveFields) {
        if (changeLog == null) {
            return null;
        }
        return changeLog.toBuilder()
                .histories(removeSensitiveLogEvents(changeLog.getHistories(), sensitiveFields))
                .values(removeSensitiveLogEvents(changeLog.getValues(), sensitiveFields))
                .build();
    }

    private static List<JiraIssueChangeLog.ChangeLogEvent> removeSensitiveLogEvents(List<JiraIssueChangeLog.ChangeLogEvent> events,
                                                                                    List<String> sensitiveFields) {
        if (CollectionUtils.isEmpty(events)) {
            return Collections.emptyList();
        }
        return events.stream().map(changeLogEvent -> changeLogEvent.toBuilder()
                        .author(sanitizeJiraUser(changeLogEvent.getAuthor(), sensitiveFields))
                        .items(removeSensitiveLogDetails(changeLogEvent.getItems(), sensitiveFields))
                        .build())
                .collect(Collectors.toList());
    }

    private static List<JiraIssueChangeLog.ChangeLogDetails> removeSensitiveLogDetails(List<JiraIssueChangeLog.ChangeLogDetails> logDetails,
                                                                                       List<String> sensitiveFields) {
        if (CollectionUtils.isEmpty(logDetails)) {
            return Collections.emptyList();
        }
        return logDetails.stream()
                .map(changeLogDetails -> removeSensitiveLogDetails(changeLogDetails, sensitiveFields))
                .collect(Collectors.toList());
    }

    private static JiraIssueChangeLog.ChangeLogDetails removeSensitiveLogDetails(JiraIssueChangeLog.ChangeLogDetails details,
                                                                                 List<String> sensitiveFields) {
        JiraIssueChangeLog.ChangeLogDetails.ChangeLogDetailsBuilder changeLogDetailsBuilder = details.toBuilder();
        sensitiveFields.forEach(sensitiveField -> {
            if (sensitiveField.equalsIgnoreCase(details.getFieldId()) || sensitiveField.equalsIgnoreCase(details.getField())) {
                changeLogDetailsBuilder.fromString(null)
                        .from(null)
                        .toString(null)
                        .to(null);
            }
        });
        return changeLogDetailsBuilder.build();
    }
}
