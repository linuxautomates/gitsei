package io.levelops.commons.databases.models.database.scm.converters.gerrit;

import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.integrations.gerrit.models.AccountInfo;
import io.levelops.integrations.gerrit.models.ChangeInfo;
import io.levelops.integrations.gerrit.models.ChangeInfo.LabelInfo.ApprovalInfo;
import io.levelops.integrations.gerrit.models.CommitInfo;
import io.levelops.integrations.gerrit.models.ProjectInfo.TagInfo.GitPersonInfo;
import io.levelops.integrations.gerrit.models.RevisionInfo;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

import static io.levelops.commons.databases.models.database.scm.DbScmUser.UNKNOWN;

public class GerritUserConverters {

    public static DbScmUser fromGerritPullRequestCreator(ChangeInfo source, String integrationId) {
        String cloudId = Optional.of(source.getOwner())
                .map(AccountInfo::getEmail)
                .orElse(UNKNOWN);
        String displayName = Optional.of(source.getOwner())
                .map(accountInfo -> StringUtils.firstNonBlank(accountInfo.getName(), accountInfo.getDisplayName()))
                .orElse(UNKNOWN);
        return DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(cloudId)
                .displayName(displayName)
                .originalDisplayName(displayName)
                .build();
    }

    public static Optional<DbScmUser> parseGerritPrAssignee(ChangeInfo source, String integrationId) {
        if (source.getAssignee() == null) {
            return Optional.empty();
        }
        String cloudId = StringUtils.firstNonBlank(
                source.getAssignee().getEmail(),
                DbScmPullRequest.UNKNOWN);
        String displayName = StringUtils.firstNonBlank(
                source.getAssignee().getDisplayName(),
                source.getAssignee().getName(),
                DbScmPullRequest.UNKNOWN);
        return Optional.of(DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(cloudId)
                .displayName(displayName)
                .originalDisplayName(displayName)
                .build());
    }

    public static DbScmUser fromGerritPullRequestReviewer(ApprovalInfo source, String integrationId) {
        String cloudId = StringUtils.firstNonBlank(
                source.getEmail(),
                UNKNOWN);
        String displayName = StringUtils.firstNonBlank(
                source.getName(),
                source.getUsername(),
                UNKNOWN);
        return DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(cloudId)
                .displayName(displayName)
                .originalDisplayName(displayName)
                .build();
    }

    public static DbScmUser fromGerritCommitCommitter(RevisionInfo source, String integrationId) {
        return fromGerritCommitInfo(integrationId, Optional.ofNullable(source.getCommit()).map(CommitInfo::getCommitter));
    }

    public static DbScmUser fromGerritCommitAuthor(RevisionInfo source, String integrationId) {
        return fromGerritCommitInfo(integrationId, Optional.ofNullable(source.getCommit()).map(CommitInfo::getAuthor));
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static DbScmUser fromGerritCommitInfo(String integrationId, Optional<GitPersonInfo> commitInfo) {
        String cloudId = commitInfo
                .map(GitPersonInfo::getEmail)
                .orElse(UNKNOWN);
        String displayName = commitInfo
                .map(GitPersonInfo::getName)
                .orElse(UNKNOWN);
        return DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(cloudId)
                .displayName(displayName)
                .originalDisplayName(displayName)
                .build();
    }

}
