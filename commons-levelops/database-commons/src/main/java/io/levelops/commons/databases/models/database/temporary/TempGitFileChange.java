package io.levelops.commons.databases.models.database.temporary;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.hash.Hashing;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.integrations.bitbucket.models.BitbucketCommit;
import io.levelops.integrations.github.models.GithubCommit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Log4j2
public class TempGitFileChange {
    @JsonProperty("id")
    private String id;

    @JsonProperty("file_name")
    private String fileName;

    @JsonProperty("additions")
    private Integer additions;

    @JsonProperty("changes")
    private Integer changes;

    @JsonProperty("deletions")
    private Integer deletions;

    @JsonProperty("author_name")
    private String authorName;

    @JsonProperty("author_date")
    private Long authorDate;

    @JsonProperty("commit_sha")
    private String commitSha;

    @JsonProperty("status")
    private String status;

    @JsonProperty("committer_name")
    private String committerName;

    @JsonProperty("committer_date")
    private Long committerDate;

    @JsonProperty("repo_name")
    private String repoName;

    public static List<TempGitFileChange> fromGithubCommit(GithubCommit source, String repoName) {
        if (source == null || repoName == null || source.getFiles().isEmpty()) {
            log.warn("NULL data provided for github commit parsing.");
            return Collections.emptyList();
        }
        return IterableUtils.parseIterable(source.getFiles().get(),
                file -> TempGitFileChange.builder()
                        .additions(file.getAdditions())
                        .deletions(file.getDeletions())
                        .changes(file.getChanges())
                        .authorName(source.getGitAuthor().getName())
                        .authorDate(DateUtils.toEpochSecond(source.getGitAuthor().getDate()))
                        .committerDate(DateUtils.toEpochSecond(source.getGitCommitter().getDate()))
                        .committerName(source.getGitCommitter().getName())
                        .repoName(repoName)
                        .commitSha(source.getSha())
                        .status(file.getStatus())
                        .fileName(file.getFilename())
                        .id(Hashing.sha256()
                                .hashString(source.getSha() + file.getFilename(),
                                        StandardCharsets.UTF_8)
                                .toString())
                        .build());
    }

    public static List<TempGitFileChange> fromBitbucketCommit(BitbucketCommit source, String repoName) {
        if (source == null || repoName == null || source.getDiffStats() == null) {
            log.warn("NULL data provided for github commit parsing.");
            return Collections.emptyList();
        }
        return IterableUtils.parseIterable(source.getDiffStats(),
                file -> {
            final String fileName = (file.getNewFile() != null) ? file.getNewFile().getPath() : file.getOldFile().getPath();
            return TempGitFileChange.builder()
                        .additions(file.getLinesAdded())
                        .deletions(file.getLinesRemoved())
                        .changes(0)
                        .authorName(source.getAuthor().getRaw())
                        .authorDate(DateUtils.toEpochSecond(source.getDate()))
                        .committerDate(DateUtils.toEpochSecond(source.getDate()))
                        .committerName(source.getAuthor().getRaw())
                        .repoName(repoName)
                        .commitSha(source.getHash())
                        .status(file.getStatus())
                        .fileName(fileName)
                        .id(Hashing.sha256()
                                .hashString(source.getHash() + fileName,
                                        StandardCharsets.UTF_8)
                                .toString())
                        .build();
        });
    }
}