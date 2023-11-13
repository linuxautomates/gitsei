package io.levelops.commons.databases.models.database.scm.converters.devops;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmReview;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.utils.ScmIssueCorrelationUtil;
import io.levelops.commons.dates.DateUtils;
import io.levelops.integrations.azureDevops.models.IdentityRef;
import io.levelops.integrations.azureDevops.models.Label;
import io.levelops.integrations.azureDevops.models.Project;
import io.levelops.integrations.azureDevops.models.PullRequest;
import io.levelops.integrations.azureDevops.models.Repository;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.database.scm.DbScmPullRequest.COMMENTED;
import static io.levelops.commons.databases.models.database.scm.DbScmPullRequest.UNKNOWN;

public class AzureDevOpsPullRequestConverters {

    private static final Map<String, String> vote = Map.of("10", "approved", "5", "approved with suggestions",
            "0", "no vote", "-5", "waiting for author", "-10", "rejected");

    public static final String FORWARD_SLASH = "/";
    public static final String PR_LINK = "pr_link";
    public static final String NULL_PR_LINK = "#";
    public static final String ADO_PR = "/pullrequest/";

    public static DbScmPullRequest fromAzureDevopsPullRequest(PullRequest pullRequest,
                                                              Project project,
                                                              Repository repository,
                                                              String integrationId) {
        List<String> sourceBranchPaths = Arrays.asList(pullRequest.getSourceRefName().split(FORWARD_SLASH));
        List<String> targetBranchPaths = Arrays.asList(pullRequest.getTargetRefName().split(FORWARD_SLASH));
        PullRequest.IdentityRef identityRef = pullRequest.getCreatedBy();
        DbScmUser assigneeInfo = DbScmUser.builder().cloudId(MoreObjects.firstNonNull(identityRef.getUniqueName(), UNKNOWN))
                .displayName(MoreObjects.firstNonNull(identityRef.getDisplayName(), UNKNOWN))
                .originalDisplayName(MoreObjects.firstNonNull(identityRef.getDisplayName(), UNKNOWN))
                .integrationId(integrationId)
                .build();

        Map<String, Object> metadata = new HashMap<>();
        if (StringUtils.isNotEmpty(repository.getWebUrl())) {
            String prLink = repository.getWebUrl();
            metadata.put(PR_LINK, prLink == null ? NULL_PR_LINK : prLink + ADO_PR + pullRequest.getPullRequestId());
        }
        return DbScmPullRequest.builder()
                .state(pullRequest.getStatus())
                .repoIds(List.of(pullRequest.getRepository().getName()))
                .workitemIds(ScmIssueCorrelationUtil.extractWorkitems(pullRequest.getTitle(), sourceBranchPaths.get(sourceBranchPaths.size() - 1)))
                .issueKeys(ScmIssueCorrelationUtil.extractJiraKeys(pullRequest.getTitle(), sourceBranchPaths.get(sourceBranchPaths.size() - 1)))
                .title(MoreObjects.firstNonNull(pullRequest.getTitle(), ""))
                .number(String.valueOf(pullRequest.getPullRequestId()))//
                .integrationId(integrationId)
                .assignees(List.of(pullRequest.getCreatedBy().getDisplayName()))
                .assigneesInfo(List.of(assigneeInfo))
                .labels(pullRequest.getLabels().stream().map(Label::getName).collect(Collectors.toList()))
                .reviews(getReviewersInfo(pullRequest, integrationId))
                .sourceBranch(sourceBranchPaths.get(sourceBranchPaths.size() - 1))
                .targetBranch(targetBranchPaths.get(targetBranchPaths.size() - 1))
                .creator(pullRequest.getCreatedBy().getDisplayName())
                .creatorInfo(DbScmUser.fromAzureDevopsPullRequestCreator(pullRequest, integrationId))
                .mergeSha(pullRequest.getLastMergeCommit() != null ? pullRequest.getLastMergeCommit().getCommitId() : null)
                .project(project.getName())
                .commitShas(pullRequest.getCommits().stream()
                        .map(PullRequest.CommitInfo::getCommitId).collect(Collectors.toList()))
                .merged(pullRequest.getClosedDate() != null ? Boolean.TRUE : Boolean.FALSE)
                .prCreatedAt(DateUtils.parseDateTime(pullRequest.getCreationDate()).getEpochSecond())
                .prUpdatedAt(pullRequest.getLastMergeCommit() != null ?
                        DateUtils.parseDateTime(pullRequest.getLastMergeCommit().getCommitter().getDate()).getEpochSecond()
                        : DateUtils.parseDateTime(pullRequest.getCreationDate()).getEpochSecond())
                .prMergedAt(pullRequest.getClosedDate() != null ? DateUtils.parseDateTime(pullRequest.getClosedDate()).getEpochSecond() : null)
                .prClosedAt(pullRequest.getClosedDate() != null ? DateUtils.parseDateTime(pullRequest.getClosedDate()).getEpochSecond() : null)
                .metadata(metadata)
                .build();
    }

    private static List<DbScmReview> getReviewersInfo(PullRequest pullRequest, String integrationId) {
        return ListUtils.emptyIfNull(pullRequest.getPullRequestHistories()).stream()
                .flatMap(history -> history.getComments().stream()
                        .map(comment -> {
                            IdentityRef identityRef = comment.getIdentities();
                            String reviewerName = identityRef.getUniqueName();
                            long reviewedAt = DateUtils.parseDateTime(MoreObjects.firstNonNull(history.getLastUpdatedDate(), history.getLastUpdatedDate())).getEpochSecond();
                            String state = null;
                            if ("text".equals(comment.getCommentType())) {
                                state = COMMENTED;
                            } else if ("system".equals(comment.getCommentType()) && history.getProperty().getCodeReviewVoteResult() != null &&
                                    history.getProperty().getCodeReviewVoteResult().getValue() != null) {
                                state = vote.get(history.getProperty().getCodeReviewVoteResult().getValue());
                            }
                            return state == null ? null : DbScmReview.builder()
                                    .reviewerInfo(DbScmUser.fromAzureDevopsPullRequestReviewer(identityRef, integrationId))
                                    .reviewId(reviewerName + "-" + reviewedAt)
                                    .reviewer(reviewerName)
                                    .state(state)
                                    .reviewedAt(reviewedAt)
                                    .build();
                        }))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
