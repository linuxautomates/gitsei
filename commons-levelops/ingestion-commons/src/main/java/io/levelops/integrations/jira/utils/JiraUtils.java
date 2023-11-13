package io.levelops.integrations.jira.utils;

import io.levelops.integrations.jira.models.JiraComment;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.jira.models.JiraIssueChangeLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class JiraUtils {

    public static Long extractFirstCommentTimeFromJiraIssue(JiraIssue jiraIssue) {
        Long firstCommentTime = extractFirstCommentTimeFromChangeLog(jiraIssue);
        return extractFirstCommentTimeFromComments(jiraIssue, firstCommentTime);
    }

    private static Long extractFirstCommentTimeFromChangeLog(JiraIssue jiraIssue) {
        Long firstCommentTime = null;
        if (jiraIssue.getChangeLog() == null || CollectionUtils.isEmpty(jiraIssue.getChangeLog().getHistories())) {
            return firstCommentTime;
        }
        // sorting events from newest to oldest
        List<JiraIssueChangeLog.ChangeLogEvent> sortedEvents = new ArrayList<>(jiraIssue.getChangeLog().getHistories());
        sortedEvents.sort((o1, o2) ->
                (o1.getCreated() != null && o2.getCreated() != null) ?
                        (int) (o2.getCreated().toInstant().getEpochSecond()
                                - o1.getCreated().toInstant().getEpochSecond()) : 0);
        for (JiraIssueChangeLog.ChangeLogEvent event : sortedEvents) {
            long logCreatedAt = event.getCreated().toInstant().getEpochSecond();
            for (JiraIssueChangeLog.ChangeLogDetails item : event.getItems()) {
                if ("comment".equalsIgnoreCase(StringUtils.trimToEmpty(item.getField()))) {
                    if (firstCommentTime == null || firstCommentTime > logCreatedAt) {
                        firstCommentTime = logCreatedAt;
                    }
                }
            }
        }
        return firstCommentTime;
    }

    private static Long extractFirstCommentTimeFromComments(JiraIssue jiraIssue, Long firstCommentTime) {
        if (jiraIssue.getFields().getComment() == null
                || CollectionUtils.isEmpty(jiraIssue.getFields().getComment().getComments())) {
            return firstCommentTime;
        }
        for (JiraComment c : jiraIssue.getFields().getComment().getComments()) {
            if (c.getCreated() != null
                    && (firstCommentTime == null || c.getCreated().toInstant().getEpochSecond() < firstCommentTime)) {
                firstCommentTime = c.getCreated().toInstant().getEpochSecond();
            }
        }
        return firstCommentTime;
    }

}
