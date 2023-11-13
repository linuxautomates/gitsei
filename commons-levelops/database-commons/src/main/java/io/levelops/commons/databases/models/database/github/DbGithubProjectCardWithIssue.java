package io.levelops.commons.databases.models.database.github;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DbGithubProjectCardWithIssue {

    @JsonProperty("current_column_id")
    private String currentColumnId;

    @JsonProperty("card_id")
    private String cardId;

    @JsonProperty("archived")
    private Boolean archived;

    @JsonProperty("creator")
    private String creator;

    @JsonProperty("content_url")
    private String contentUrl;

    @JsonProperty("issue_id")
    private String issueId;

    @JsonProperty("card_created_at")
    private Long cardCreatedAt;

    @JsonProperty("card_updated_at")
    private Long cardUpdatedAt;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("updated_at")
    private Long updatedAt;

    @JsonProperty("issues_number")
    private String number;

    @JsonProperty("issues_repo_id")
    private String repoId; //repository id -- something like: "levelops/api-levelops"

    @JsonProperty("issues_labels")
    private List<String> labels;

    @JsonProperty("issues_assignees")
    private List<String> assignees;

    @JsonProperty("issue_title")
    private String title;

    @JsonProperty("issue_state")
    private String state;

    @JsonProperty("issue_created_at")
    private Long issueCreatedAt;

    @JsonProperty("issue_closed_at")
    private Long issueClosedAt;
}
