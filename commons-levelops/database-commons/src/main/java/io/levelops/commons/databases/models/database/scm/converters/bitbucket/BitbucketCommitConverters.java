package io.levelops.commons.databases.models.database.scm.converters.bitbucket;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.VCS_TYPE;
import io.levelops.commons.databases.utils.ScmIssueCorrelationUtil;
import io.levelops.integrations.bitbucket.models.BitbucketCommit;
import io.levelops.integrations.bitbucket.models.BitbucketCommitDiffStat;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.levelops.commons.databases.models.database.scm.DbScmCommit.COMMIT_MESSAGE_MAX_LENGTH;

public class BitbucketCommitConverters {

    public static DbScmCommit fromBitbucketCommit(BitbucketCommit source, String repoId, String projectName, String integrationId, Long eventTime) {
        DbScmUser authorInfo = BitbucketUserConverters.fromBitbucketCommit(integrationId, source);
        List<String> workitemIds = ScmIssueCorrelationUtil.extractWorkitems(source.getMessage());
        List<String> jiraIssueKeys = ScmIssueCorrelationUtil.extractJiraKeys(source.getMessage());
        return DbScmCommit.builder()
                .integrationId(integrationId)
                .vcsType(VCS_TYPE.GIT)
                .commitSha(source.getHash())
                .issueKeys(jiraIssueKeys)
                .workitemIds(workitemIds)
                .repoIds(List.of(repoId))
                .project(MoreObjects.firstNonNull(projectName, repoId))
                .committer(authorInfo.getDisplayName())
                .committerInfo(authorInfo)
                .commitUrl(source.getLinks().getHtml().getHref())
                .author(authorInfo.getDisplayName())
                .authorInfo(authorInfo)
                .message(StringUtils.truncate(source.getMessage(), COMMIT_MESSAGE_MAX_LENGTH))
                .filesCt(CollectionUtils.size(source.getDiffStats()))
                .additions(source.getDiffStats() != null ? source.getDiffStats().stream().mapToInt(BitbucketCommitDiffStat::getLinesAdded).sum() : 0)
                .deletions(source.getDiffStats() != null ? source.getDiffStats().stream().mapToInt(BitbucketCommitDiffStat::getLinesRemoved).sum() : 0)
                .changes(0) //LEV-3306 - We don't get lines changed from bitbucket
                .committedAt(source.getDate().toInstant().getEpochSecond())
                .ingestedAt(DateUtils.truncate(new Date(TimeUnit.MILLISECONDS.toMillis(eventTime)), Calendar.DATE).toInstant().getEpochSecond())
                .directMerge(false)
                .build();
    }

}
