package io.levelops.commons.databases.models.database.scm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.utils.ScmDiffParserUtils;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.models.ChangeVolumeStats;
import io.levelops.integrations.azureDevops.models.Change;
import io.levelops.integrations.azureDevops.models.ChangeSet;
import io.levelops.integrations.azureDevops.models.Commit;
import io.levelops.integrations.bitbucket.models.BitbucketCommitDiffStat;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerFile;
import io.levelops.integrations.gerrit.models.RevisionInfo;
import io.levelops.integrations.github.models.GithubCommitFile;
import io.levelops.integrations.gitlab.models.GitlabChange;
import io.levelops.integrations.helixcore.models.HelixCoreFile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Log4j2
public class DbScmFileCommit {

    @JsonProperty("id")
    private String id;

    @JsonProperty("file_id")
    private String fileId;

    @JsonProperty("commit_sha")
    private String commitSha;

    @JsonProperty("change")
    private Integer change;

    @JsonProperty("addition")
    private Integer addition;

    @JsonProperty("deletion")
    private Integer deletion;

    @JsonProperty("committed_at")
    private Long committedAt;
    
    @JsonProperty("previous_committed_at")
    private Long previousCommittedAt;

    @JsonProperty("createdAt")
    private Long createdAt;

    public static DbScmFileCommit fromGithubCommitFile(GithubCommitFile source,
                                                       String commitSha,
                                                       Long eventTime) {
        return DbScmFileCommit.builder()
                .addition(source.getAdditions())
                .change(source.getChanges())
                .deletion(source.getDeletions())
                .commitSha(commitSha)
                .committedAt(eventTime)
                .build();
    }

    public static DbScmFileCommit fromAzureDevopsCommitFileInfo(Change change, String commitSha, Commit.GitUserDate committer) {
        String changeType = change.getChangeType();
        return DbScmFileCommit.builder()
                .addition("add".equals(changeType) ? 1 : 0)
                .change("edit".equals(changeType) ? 1 : 0)
                .deletion("delete".equals(changeType) ? 1 : 0)
                .commitSha(commitSha)
                .committedAt(committer != null ? DateUtils.parseDateTime(committer.getDate()).getEpochSecond() : null)
                .build();
    }

    public static DbScmFileCommit fromAzureDevopsChangesetsFileInfo(ChangeSet source) {
        Instant createdDate = DateUtils.parseDateTime(source.getCreatedDate());
        return DbScmFileCommit.builder()
                .addition(0)
                .change(0)
                .deletion(0)
                .commitSha(String.valueOf(source.getChangesetId()))
                .committedAt(source.getCheckedInBy() != null && createdDate != null? createdDate.getEpochSecond() : null)
                .build();
    }

    public static DbScmFileCommit fromBitbucketStat(BitbucketCommitDiffStat source,
                                                    String commitSha,
                                                    Long eventTime) {
        return DbScmFileCommit.builder()
                .addition(source.getLinesAdded())
                .change(source.getLinesAdded() + source.getLinesRemoved())
                .deletion(source.getLinesRemoved())
                .commitSha(commitSha)
                .committedAt(TimeUnit.MILLISECONDS.toSeconds(eventTime))
                .build();
    }

    public static DbScmFileCommit fromBitbucketServerCommitFile(BitbucketServerFile source,
                                                                String commitSha,
                                                                Long eventTime) {
        int linesChanged = (source.getLinesAdded() == null ? 0 : source.getLinesAdded())
                + (source.getLinesRemoved() == null ? 0 : source.getLinesRemoved());
        return DbScmFileCommit.builder()
                .addition(source.getLinesAdded())
                .deletion(source.getLinesRemoved())
                .change(linesChanged)
                .commitSha(commitSha)
                .committedAt(eventTime)
                .build();
    }


    public static DbScmFileCommit fromGitlabCommitFile(GitlabChange source,
                                                       String commitSha,
                                                       Long eventTime) {
        ChangeVolumeStats diffStats;
        try {
            diffStats = ScmDiffParserUtils.fromGitlabDiff(source);
        } catch (Exception e) {
            log.error("Failed to parse diff for commit:{}, file:{}", commitSha, source.getNewPath(), e);
            diffStats = ChangeVolumeStats.builder().fileName(source.getNewPath()).additions(0).deletions(0).changes(0).build();
        }
        return DbScmFileCommit.builder()
                .commitSha(commitSha)
                .committedAt(eventTime)
                .createdAt((new Date()).toInstant().getEpochSecond())
                .change(diffStats.getChanges())
                .addition(diffStats.getAdditions())
                .deletion(diffStats.getDeletions())
                .build();
    }

    public static DbScmFileCommit fromHelixCoreCommitFile(HelixCoreFile source,
                                                          String commitSha,
                                                          Long eventTime) {
        return DbScmFileCommit.builder()
                .commitSha(commitSha)
                .committedAt(eventTime)
                .addition(source.getAdditions())
                .deletion(source.getDeletions())
                .change(source.getChanges())
                .build();
    }
}
