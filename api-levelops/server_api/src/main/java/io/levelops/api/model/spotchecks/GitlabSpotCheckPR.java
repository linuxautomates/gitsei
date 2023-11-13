package io.levelops.api.model.spotchecks;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.integrations.gitlab.models.GitlabMergeRequest;
import lombok.Builder;
import lombok.Value;

import static io.levelops.api.model.spotchecks.DateUtils.gitlabDateToString;

@Value
@Builder(toBuilder = true)
public class GitlabSpotCheckPR {

    @JsonProperty("number")
    String number;

//    @JsonProperty("url")
//    String url;
    @JsonProperty("title")
    String title;
    @JsonProperty("state")
    String state;
    @JsonProperty("created_at")
    String createdAt;
    @JsonProperty("updated_at")
    String updatedAt;
    @JsonProperty("closed_at")
    String closedAt;
    @JsonProperty("merged_at")
    String mergedAt;

    @JsonProperty("author_id")
    String authorId;
    @JsonProperty("author")
    String author;
    @JsonProperty("author_name")
    String authorName;

    public static GitlabSpotCheckPR fromGitlabPR(GitlabMergeRequest pr) {
        GitlabSpotCheckPR spotCheckPR = GitlabSpotCheckPR.builder()
                .number(pr.getIid())
                .title(pr.getTitle())
                .state(pr.getState())
                .createdAt(gitlabDateToString(pr.getCreatedAt()))
                .updatedAt(gitlabDateToString(pr.getUpdatedAt()))
                .mergedAt(gitlabDateToString(pr.getMergedAt()))
                .closedAt(gitlabDateToString(pr.getClosedAt()))
                .authorId(pr.getAuthor().getId())
                .author(pr.getAuthor().getUsername())
                .authorName(pr.getAuthor().getName())
                .build();
        return spotCheckPR;
    }
}
