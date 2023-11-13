package io.levelops.commons.faceted_search.db.models.scm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder(toBuilder = true)
@JsonDeserialize(builder = EsScmIssue.EsScmIssueBuilder.class)
public class EsScmIssue {

    static final String UNKNOWN = "_UNKNOWN_";

    @JsonProperty("i_id")
    private String id;

    @JsonProperty("i_integration_id")
    private String integrationId;

    @JsonProperty("i_repo_id")
    private String repoId;

    @JsonProperty("i_project")
    private String project;

    @JsonProperty("i_issue_id")
    private String issueId;

    @JsonProperty("i_number")
    private String number;

    @JsonProperty("i_creator")
    private String creator;

    @JsonProperty("i_creator_info")
    private DbScmUser creatorInfo; //contains DbScmUser object for creator

    @JsonProperty("i_assignees")
    private List<String> assignees;

    @JsonProperty("i_labels")
    private List<String> labels;

    @JsonProperty("i_state")
    private String state;

    @JsonProperty("i_title")
    private String title;

    @JsonProperty("i_url")
    private String url;

    @JsonProperty("i_num_comments")
    private Integer numComments;

    @JsonProperty("i_issue_created_at")
    private Long issueCreatedAt;

    @JsonProperty("i_issue_updated_at")
    private Long issueUpdatedAt;

    @JsonProperty("i_issue_closed_at")
    private Long issueClosedAt;

    @JsonProperty("i_first_comment_at")
    private Long firstCommentAt;

    @JsonProperty("i_created_at")
    private Long createdAt;

    @JsonProperty("i_creator_id")
    private String creatorId;

    @JsonProperty("i_response_time")
    private Long responseTime;

    @JsonProperty("i_solve_time")
    private Long solveTime;
}
