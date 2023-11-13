package io.levelops.commons.databases.models.database.scm.converters.gerrit;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmReview;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.utils.ScmIssueCorrelationUtil;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.utils.ListUtils;
import io.levelops.commons.utils.MapUtils;
import io.levelops.integrations.gerrit.models.ChangeInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.database.scm.DbScmPullRequest.UNKNOWN;

public class GerritPullRequestConverters {

    private static final String NEW_STATE = "NEW"; // Gerrit only has 3 states: NEW, MERGED, ABANDONED
    private static final String CODE_REVIEW_LABEL = "Code-Review";

    public static DbScmPullRequest parsePullRequest(String integrationId, ChangeInfo source) {
        Validate.notBlank(integrationId, "integrationId cannot be null or empty.");
        Validate.notNull(source, "source cannot be null.");

        Optional<DbScmUser> assigneeOpt = GerritUserConverters.parseGerritPrAssignee(source, integrationId);
        List<DbScmUser> assigneesInfo = assigneeOpt
                .map(List::of)
                .orElse(Collections.emptyList());
        List<String> assignees = assigneeOpt
                .map(DbScmUser::getDisplayName)
                .map(List::of)
                .orElse(Collections.emptyList());

        DbScmUser creator = GerritUserConverters.fromGerritPullRequestCreator(source, integrationId);

        List<DbScmReview> reviews = parsePrReviews(source, integrationId);

        boolean merged = source.getSubmitter() != null;

        // for Gerrit, we only want to consider the last revision after a PR is merged (before that, the work is in progress)
        List<String> commitShas = (merged && source.getCurrentRevision() != null) ? List.of(source.getCurrentRevision()) : List.of();

        Set<String> issueKeys = new HashSet<>();
        Set<String> workitemIds = new HashSet<>();
        parseTicketIds(source, issueKeys, workitemIds);

        String repoId = StringUtils.defaultString(source.getProject());
        Long createdAt = DateUtils.toEpochSecond(source.getCreated());
        Long updatedAt = DateUtils.toEpochSecond(source.getUpdated());
        Long mergedAt = DateUtils.toEpochSecond(source.getSubmitted());
        boolean closed = !NEW_STATE.equalsIgnoreCase(source.getStatus()); // MERGED or ABANDONED
        Long closedAt = closed ? updatedAt : null;

        return DbScmPullRequest.builder()
                .integrationId(integrationId)
                .number(String.valueOf(source.getNumber()))
                .title(source.getSubject())
                .state(StringUtils.firstNonBlank(source.getStatus(), UNKNOWN))
                .merged(merged)
                .workitemIds(new ArrayList<>(workitemIds))
                .issueKeys(new ArrayList<>(issueKeys))
                .assignees(assignees)
                .assigneesInfo(assigneesInfo)
                .labels(Collections.emptyList())
                .reviews(reviews)
                .sourceBranch(source.getBranch())
                .targetBranch(UNKNOWN)
                .creator(creator.getDisplayName())
                .creatorInfo(creator)
                .mergeSha(source.getSubmitter() != null ? source.getCurrentRevision() : null)
                .repoIds(List.of(repoId))
                .project(repoId)
                .commitShas(commitShas)
                .prCreatedAt(createdAt)
                .prUpdatedAt(updatedAt)
                .prMergedAt(mergedAt)
                .prClosedAt(closedAt)
                .build();
    }

    private static void parseTicketIds(ChangeInfo source, Set<String> issueKeys, Set<String> workitemIds) {
        workitemIds.addAll(ScmIssueCorrelationUtil.extractWorkitems(source.getSubject(), source.getBranch()));
        issueKeys.addAll(ScmIssueCorrelationUtil.extractJiraKeys(source.getSubject(), source.getBranch()));
        for (ChangeInfo.ChangeMessageInfo messageInfo : ListUtils.emptyIfNull(source.getMessages())) {
            workitemIds.addAll(ScmIssueCorrelationUtil.extractWorkitems(messageInfo.getMessage()));
            issueKeys.addAll(ScmIssueCorrelationUtil.extractJiraKeys(messageInfo.getMessage()));
        }
    }

    private static List<DbScmReview> parsePrReviews(ChangeInfo source, String integrationId) {
        ChangeInfo.LabelInfo codeReviewLabelInfo = source.getLabels().get(CODE_REVIEW_LABEL);
        if (codeReviewLabelInfo == null || CollectionUtils.isEmpty(codeReviewLabelInfo.getAll())) {
            return Collections.emptyList();
        }

        Map<String, String> reviewLabels = MapUtils.emptyIfNull(codeReviewLabelInfo.getValues()).entrySet().stream()
                .collect(Collectors.toMap(
                        kv -> kv.getKey().replaceAll("[ +]", ""),
                        Map.Entry::getValue,
                        (a, b) -> b));

        return codeReviewLabelInfo.getAll()
                .stream()
                .filter(approvalInfo -> approvalInfo.getDate() != null)
                .map(approvalInfo -> DbScmReview.builder()
                        .reviewerInfo(GerritUserConverters.fromGerritPullRequestReviewer(approvalInfo, integrationId))
                        .reviewer(approvalInfo.getName())
                        .reviewId(String.valueOf(approvalInfo.hashCode()))
                        .reviewedAt(DateUtils.toEpochSecond(approvalInfo.getDate()))
                        .state(MoreObjects.firstNonNull(reviewLabels.get(approvalInfo.getValue()), UNKNOWN)) //review
                        .build())
                .collect(Collectors.toList());
    }
}
