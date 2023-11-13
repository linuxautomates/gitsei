package io.levelops.aggregations.models.gerrit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.aggregations.models.github.GithubRepoAggData;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Setter
@Getter
@ToString
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GerritRepoAggData {
    @JsonProperty("repo_name")
    private String repoName;

    @JsonProperty("users_by_commits")
    private List<GithubRepoAggData.UserMetrics> repoUsersByCommitCt = new ArrayList<>();

    @JsonProperty("users_by_prs")
    private List<GithubRepoAggData.UserMetrics> repoUsersByPrCt = new ArrayList<>();

    @JsonProperty("files_changed")
    private List<GithubRepoAggData.ChangedFileMetrics> fileMetrics = new ArrayList<>();

    @JsonProperty("reviewed_prs")
    private Integer reviewedPrs = 0;

    @JsonProperty("non_reviewed_prs")
    private Integer nonReviewedPrs = 0;

    @Setter
    @Getter
    @ToString
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserMetrics {
        @JsonProperty("user")
        private String user;
        @JsonProperty("count") // could refer to commits or prs
        private Integer count;
    }

    @Setter
    @Getter
    @ToString
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ChangedFileMetrics {
        @JsonProperty("file_name")
        private String fileName;
        @JsonProperty("lines_added")
        private Integer linesAdded;
        @JsonProperty("lines_deleted")
        private Integer linesDeleted;
        @JsonProperty("lines_changed")
        private Integer linesChanged;
        @JsonProperty("commits_count")
        private Integer commitsCount;
        @JsonProperty("changing_users")
        private Set<String> changingUsers;
    }
}
