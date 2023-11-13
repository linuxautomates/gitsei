package io.levelops.aggregations.models.bitbucket;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Setter
@Getter
@ToString
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BitbucketRepoAggData {
    @JsonProperty("repo_name")
    private String repoName;

    @JsonProperty("users_by_commits")
    private List<UserMetrics> repoUsersByCommitCt = new ArrayList<>();

    @JsonProperty("users_by_prs")
    private List<UserMetrics> repoUsersByPrCt = new ArrayList<>();

    @JsonProperty("files_changed")
    private List<ChangedFileMetrics> fileMetrics = new ArrayList<>();

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
