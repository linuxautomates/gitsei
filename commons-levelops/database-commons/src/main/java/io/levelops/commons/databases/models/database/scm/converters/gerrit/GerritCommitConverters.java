package io.levelops.commons.databases.models.database.scm.converters.gerrit;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.VCS_TYPE;
import io.levelops.commons.databases.utils.ScmIssueCorrelationUtil;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.utils.MapUtils;
import io.levelops.integrations.gerrit.models.ChangeInfo;
import io.levelops.integrations.gerrit.models.CommitInfo;
import io.levelops.integrations.gerrit.models.ProjectInfo;
import io.levelops.integrations.gerrit.models.ProjectInfo.TagInfo.GitPersonInfo;
import io.levelops.integrations.gerrit.models.RevisionInfo;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static io.levelops.commons.databases.models.database.scm.DbScmCommit.COMMIT_MESSAGE_MAX_LENGTH;
import static io.levelops.commons.databases.models.database.scm.DbScmCommit.UNKNOWN;

@Log4j2
public class GerritCommitConverters {

    private static final String MERGED_STATE = "MERGED";

    /**
     * Parse Gerrit ChangeInfo for Commit metadata.
     * Due to the nature of Gerrit, we need to ignore unfinished revisions.
     * To do that, we will only parse commits that are merged.
     */
    public static Optional<DbScmCommit> parseCommit(String integrationId, ChangeInfo changeInfo) {
        Validate.notBlank(integrationId, "integrationId cannot be null or empty.");
        Validate.notNull(changeInfo, "changeInfo cannot be null.");

        boolean merged = MERGED_STATE.equalsIgnoreCase(changeInfo.getStatus());
        if (!merged) {
            // for Gerrit, we only want to consider the last revision after a PR is merged (before that, the work is in progress)
            log.debug("Ignoring commit from Gerrit Change id={}: not merged yet", changeInfo.getId());
            return Optional.empty();
        }

        String currentRevisionId = changeInfo.getCurrentRevision();
        if (currentRevisionId == null || changeInfo.getRevisions() == null || !changeInfo.getRevisions().containsKey(currentRevisionId)) {
            log.warn("Could not find commit in Change id={}: current revision info missing", changeInfo.getId());
            return Optional.empty();
        }

        return Optional.ofNullable(parseCommitByRevisionId(integrationId, changeInfo, currentRevisionId));
    }

    /**
     * Parse a commit from a revisionId. Use this for testing. For the normal flow, use:
     * {@link GerritCommitConverters#parseCommit(String, ChangeInfo)}
     */
    public static DbScmCommit parseCommitByRevisionId(String integrationId, ChangeInfo changeInfo, String revisionId) {
        Validate.notBlank(integrationId, "integrationId cannot be null or empty.");
        Validate.notNull(changeInfo, "changeInfo cannot be null.");
        Validate.notBlank(revisionId, "revisionId cannot be null or empty.");
        RevisionInfo revision = MapUtils.emptyIfNull(changeInfo.getRevisions()).get(revisionId);
        Validate.notNull(revision, "revision cannot be null.");

        String repoId = StringUtils.defaultString(changeInfo.getProject());
        DbScmUser committerInfo = GerritUserConverters.fromGerritCommitCommitter(revision, integrationId);
        DbScmUser authorInfo = GerritUserConverters.fromGerritCommitAuthor(revision, integrationId);
        String commitUrl = parseCommitUrl(revision);
        String message = parseMessage(revision);
        List<String> workitemIds = ScmIssueCorrelationUtil.extractWorkitems(message);
        List<String> jiraIssueKeys = ScmIssueCorrelationUtil.extractJiraKeys(message);

        Long eventTime = DateUtils.toEpochSecond(revision.getCreated());
        Long ingestedAt = DateUtils.truncate(eventTime, Calendar.DATE);

        return DbScmCommit.builder()
                .integrationId(integrationId)
                .vcsType(VCS_TYPE.GIT)
                .commitSha(revisionId)
                .issueKeys(jiraIssueKeys)
                .workitemIds(workitemIds)
                .repoIds(List.of(repoId))
                .project(repoId)
                .commitUrl(commitUrl)
                .committer(committerInfo.getDisplayName())
                .committerInfo(committerInfo)
                .author(authorInfo.getDisplayName())
                .authorInfo(authorInfo)
                .message(StringUtils.truncate(message, COMMIT_MESSAGE_MAX_LENGTH))
                .filesCt(CollectionUtils.size(revision.getFiles()))
                .additions(MoreObjects.firstNonNull(changeInfo.getInsertions(), 0))
                .deletions(MoreObjects.firstNonNull(changeInfo.getDeletions(), 0))
                .changes(0) // TODO LEV-3306
                .branch(changeInfo.getBranch())
                .committedAt(eventTime)
                .ingestedAt(ingestedAt)
                .directMerge(false)
                .build();
    }

    private static String parseCommitUrl(RevisionInfo source) {
        if (source.getCommit() == null || source.getCommit().getWebLinks() == null) {
            return null;
        }
        return IterableUtils.getFirst(source.getCommit().getWebLinks())
                .map(ProjectInfo.WebLinkInfo::getUrl)
                .orElse(null);
    }

    private static String parseMessage(RevisionInfo source) {
        if (source.getCommit() == null) {
            return "";
        }
        return StringUtils.firstNonBlank(source.getCommit().getMessage(), source.getCommit().getSubject(), "");
    }

}
