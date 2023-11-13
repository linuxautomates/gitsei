package io.levelops.commons.databases.models.database.scm.converters.bitbucket;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmReview;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.utils.ScmIssueCorrelationUtil;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.utils.ListUtils;
import io.levelops.integrations.bitbucket.models.BitbucketPullRequest;
import io.levelops.integrations.bitbucket.models.BitbucketPullRequestActivity;
import io.levelops.integrations.bitbucket.models.BitbucketRepoRef;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.database.scm.DbScmPullRequest.APPROVED;
import static io.levelops.commons.databases.models.database.scm.DbScmPullRequest.COMMENTED;
import static io.levelops.commons.databases.models.database.scm.DbScmPullRequest.UNKNOWN;

public class BitbucketPullRequestConverters {

    public static final String OPEN = "OPEN";
    public static final String MERGED = "MERGED";
    public static final String DECLINED = "DECLINED";

    public static final String PR_LINK = "pr_link";
    public static final String NULL_PR_LINK = "#";
    public static final String HTTPS_BITBUCKET_ORG = "https://bitbucket.org/";
    public static final String BITBUCKET_PULL = "/pull-requests/";

    public static DbScmPullRequest fromBitbucketPullRequest(BitbucketPullRequest source,
                                                            String repoId,
                                                            String projectName,
                                                            String integrationId,
                                                            String mergeCommitSha,
                                                            Integration integration) {
        List<DbScmReview> reviews = parseReviews(source, integrationId);
        List<DbScmUser> assigneesInfo = parseAssigneesInfo(source, integrationId);
        List<String> assignees = parseAssignees(assigneesInfo);
        List<String> commitShas = StringUtils.isNotEmpty(mergeCommitSha) ? List.of(mergeCommitSha) : Collections.emptyList();
        String sourceBranchName = parseBranch(source.getSource());
        String targetBranchName = parseBranch(source.getDestination());
        DbScmUser creatorInfo = BitbucketUserConverters.fromBitbucketUser(integrationId, source.getAuthor());
        Map<String, Object> metadata = new HashMap<>();
        parsePrLink(metadata, source,integration);
        return DbScmPullRequest.builder()
                .state(source.getState())
                .number(String.valueOf(source.getId()))
                .integrationId(integrationId)
                .workitemIds(ScmIssueCorrelationUtil.extractWorkitems(source.getTitle(), sourceBranchName))
                .issueKeys(ScmIssueCorrelationUtil.extractJiraKeys(source.getTitle(), sourceBranchName))
                .title(StringUtils.defaultString(source.getTitle()))
                .assignees(assignees)
                .assigneesInfo(assigneesInfo)
                .labels(Collections.emptyList())
                .reviews(reviews)
                .sourceBranch(sourceBranchName)
                .targetBranch(targetBranchName)
                .creator(creatorInfo.getDisplayName())
                .creatorInfo(creatorInfo)
                .mergeSha(mergeCommitSha)
                .repoIds(List.of(repoId))
                .project(MoreObjects.firstNonNull(projectName, repoId))
                .commitShas(commitShas)
                .merged(mergeCommitSha != null)
                .prCreatedAt(DateUtils.toEpochSecond(source.getCreatedOn()))
                .prMergedAt(source.getMergeCommit() != null ? DateUtils.toEpochSecond(source.getUpdatedOn()) : null)
                .prUpdatedAt(DateUtils.toEpochSecond(source.getUpdatedOn()))
                .prClosedAt(source.getState().equalsIgnoreCase("open") ? null : DateUtils.toEpochSecond(source.getUpdatedOn()))
                .metadata(metadata)
                .build();
    }

    public static void parsePrLink(Map<String, Object> metadata, BitbucketPullRequest source, Integration integration) {
        BitbucketPullRequest.Ref bitbucketPrRef = source.getSource();
        if (bitbucketPrRef == null) {
            return;
        }
        BitbucketRepoRef bitbucketRepoRef = bitbucketPrRef.getRepository();
        if (bitbucketRepoRef != null) {
            String prLink = bitbucketRepoRef.getFullName();
            String baseUrl = (integration != null && integration.getSatellite()) ? sanitizeUrl(integration.getUrl()) : HTTPS_BITBUCKET_ORG;
            metadata.put(PR_LINK, prLink == null ? NULL_PR_LINK : baseUrl + prLink + BITBUCKET_PULL + source.getId());
        }
    }

    private static String sanitizeUrl(String url) {
        if(url == null){
            return StringUtils.EMPTY;
        }
        if (!url.startsWith("http")) {
            url="https://"+url;
        }
        if(!url.endsWith("/")){
            url= url+"/";
        }
        return url;
    }

    public static String parseBranch(BitbucketPullRequest.Ref ref) {
        if (ref == null || ref.getBranch() == null) {
            return "";
        }
        return StringUtils.defaultString(ref.getBranch().getName());
    }

    public static List<DbScmReview> parseReviews(BitbucketPullRequest source, String integrationId) {
        return ListUtils.emptyIfNull(source.getApprovals()).stream()
                .flatMap(activity -> parseActivity(integrationId, activity).stream())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static List<DbScmReview> parseActivity(String integrationId, BitbucketPullRequestActivity activity) {
        if (activity == null) {
            return List.of();
        }
        String activityHashCode = String.valueOf(activity.hashCode());
        List<DbScmReview> reviews = new ArrayList<>();
        if (activity.getApproval() != null) {
            DbScmUser reviewerInfo = BitbucketUserConverters.fromBitbucketUser(integrationId, activity.getApproval().getUser());
            reviews.add(DbScmReview.builder()
                    .reviewerInfo(reviewerInfo)
                    .reviewId(activityHashCode)
                    .reviewer(reviewerInfo.getDisplayName())
                    .reviewedAt(DateUtils.toEpochSecond(activity.getApproval().getDate()))
                    .state(APPROVED)
                    .build());
        }
        if (activity.getComment() != null && BooleanUtils.isNotTrue(activity.getComment().getDeleted())) {
            DbScmUser reviewerInfo = BitbucketUserConverters.fromBitbucketUser(integrationId, activity.getComment().getUser());
            reviews.add(DbScmReview.builder()
                    .reviewerInfo(reviewerInfo)
                    .reviewId(String.valueOf(activity.getComment().getId()))
                    .reviewer(reviewerInfo.getDisplayName())
                    .reviewedAt(DateUtils.toEpochSecond(activity.getComment().getCreatedOn()))
                    .state(COMMENTED)
                    .build());
        }
        if (activity.getUpdate() != null && activity.getUpdate().getState() != null && !OPEN.equals(activity.getUpdate().getState())) {
            DbScmUser reviewerInfo = BitbucketUserConverters.fromBitbucketUser(integrationId, activity.getUpdate().getAuthor());
            Long date = DateUtils.toEpochSecond(activity.getUpdate().getDate());
            reviews.add(DbScmReview.builder()
                    .reviewerInfo(reviewerInfo)
                    .reviewId(activityHashCode)
                    .reviewer(reviewerInfo.getDisplayName())
                    .reviewedAt(date)
                    .state(activity.getUpdate().getState())
                    .build());
            if (StringUtils.isNotBlank(activity.getUpdate().getReason())) {
                reviews.add(DbScmReview.builder()
                        .reviewerInfo(reviewerInfo)
                        .reviewId(activityHashCode)
                        .reviewer(reviewerInfo.getDisplayName())
                        .reviewedAt(date)
                        .state(COMMENTED)
                        .build());
            }
        }
        return reviews;
    }

    public static List<DbScmUser> parseAssigneesInfo(BitbucketPullRequest source, String integrationId) {
        return source.getParticipants().stream()
                .filter(participant -> participant.getUser() != null)
                .map(participant -> BitbucketUserConverters.fromBitbucketUser(integrationId, participant.getUser()))
                .collect(Collectors.toList());
    }

    public static List<String> parseAssignees(List<DbScmUser> assigneesInfo) {
        return assigneesInfo.stream()
                .map(participant -> ObjectUtils.firstNonNull(
                        participant.getDisplayName(),
                        participant.getCloudId()))
                .filter(StringUtils::isNotEmpty)
                .distinct()
                .collect(Collectors.toList());
    }
}
