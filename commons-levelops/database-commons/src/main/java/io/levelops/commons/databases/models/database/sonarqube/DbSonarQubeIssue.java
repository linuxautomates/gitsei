package io.levelops.commons.databases.models.database.sonarqube;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.sonarqube.models.Comment;
import io.levelops.integrations.sonarqube.models.Issue;
import io.levelops.integrations.sonarqube.models.TextRange;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.lang3.time.DateUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbSonarQubeIssue.DbSonarQubeIssueBuilder.class)

public class DbSonarQubeIssue {

    private static final String UNASSIGNED = "_UNASSIGNED_";

    @JsonProperty("project")
    String project;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("pull_request")
    String pullRequest;

    @JsonProperty("type")
    String type;

    @JsonProperty("organization")
    String organization;

    @JsonProperty("key")
    String key;

    @JsonProperty("rule")
    String rule;

    @JsonProperty("severity")
    String severity;

    @JsonProperty("component")
    String component;

    @JsonProperty("line_number")
    Long lineNumber;

    @JsonProperty("text_range")
    TextRange textRange;

    @JsonProperty("status")
    String status;

    @JsonProperty("message")
    String message;

    @JsonProperty("effort")
    String effort;

    @JsonProperty("debt")
    String debt;

    @JsonProperty("author")
    String author;

    @JsonProperty("tags")
    List<String> tags;

    @JsonProperty("comments")
    List<Comment> comments;

    @JsonProperty("ingested_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZZZZ")
    Date ingestedAt;

    @JsonProperty("creationDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZZZZ")
    Date creationDate;

    @JsonProperty("updationDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZZZZ")
    Date updationDate;

    public static DbSonarQubeIssue fromIssue(@NonNull Issue issue, String integrationId, Date ingestedAt) {
        return DbSonarQubeIssue.builder()
                .project(issue.getProject())
                .author(issue.getAuthor())
                .integrationId(integrationId)
                .pullRequest(issue.getPullRequest())
                .type(issue.getType())
                .organization(issue.getOrganization())
                .key(issue.getKey())
                .rule(issue.getRule())
                .tags(issue.getTags())
                .severity(issue.getSeverity())
                .component(issue.getComponent())
                .lineNumber(issue.getLine())
                .textRange(issue.getTextRange())
                .status(issue.getStatus())
                .message(issue.getMessage())
                .effort(issue.getEffort())
                .debt(issue.getDebt())
                .comments(issue.getComments())
                .ingestedAt(DateUtils.truncate(ingestedAt, Calendar.DATE))
                .creationDate(issue.getCreationDate())
                .updationDate(issue.getUpdateDate())
                .build();
    }
}
