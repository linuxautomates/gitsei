package io.levelops.commons.databases.models.database.temporary;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.commons.dates.DateUtils;
import io.levelops.integrations.bitbucket.models.BitbucketCommit;
import io.levelops.integrations.github.models.GithubCommit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TempGitCommit {
    @JsonProperty("author_name")
    private String authorName;

    @JsonProperty("author_date")
    private Long authorDate;

    @JsonProperty("sha")
    private String sha;

    @JsonProperty("committer_name")
    private String committerName;

    @JsonProperty("committer_date")
    private Long committerDate;

    @JsonProperty("url")
    private String url;

    @JsonProperty("files_changed")
    private Integer filesChanged;

    @JsonProperty("commit_msg")
    private String commitMsg;

    @JsonProperty("repo_name")
    private String repoName;

    public static TempGitCommit fromGithubCommit(GithubCommit source, String repoName) {
        if(source == null || repoName == null)
            return null;
        return TempGitCommit.builder()
                .authorName(source.getGitAuthor().getName())
                .authorDate(DateUtils.toEpochSecond(source.getGitAuthor().getDate()))
                .sha(source.getSha())
                .committerDate(DateUtils.toEpochSecond(source.getGitCommitter().getDate()))
                .committerName(source.getGitCommitter().getName())
                .url(source.getUrl())
                .filesChanged(source.getFiles().isPresent() ? source.getFiles().get().size() : 0)
                .commitMsg(source.getMessage())
                .repoName(repoName)
                .build();
    }

    public static TempGitCommit fromBitbucketCommit(BitbucketCommit source, String repoName) {
        if(source == null || repoName == null)
            return null;
        return TempGitCommit.builder()
                .authorName(source.getAuthor().getRaw())
                .authorDate(DateUtils.toEpochSecond(source.getDate()))
                .sha(source.getHash())
                .committerDate(DateUtils.toEpochSecond(source.getDate()))
                .committerName(source.getAuthor().getRaw())
                .url(source.getLinks().getHtml().getHref())
                .filesChanged(source.getDiffStats().size())
                .commitMsg(source.getMessage())
                .repoName(repoName)
                .build();
    }
}
