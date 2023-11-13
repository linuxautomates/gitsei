package io.levelops.commons.databases.models.database.scm;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.CaseFormat;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.utils.ScmIssueCorrelationUtil;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerCommit;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerPRActivity;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerProject;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerPullRequest;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerRepository;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerUser;
import io.levelops.integrations.github.models.GithubCommit;
import io.levelops.integrations.github.models.GithubPullRequest;
import io.levelops.integrations.github.models.GithubReview;
import io.levelops.integrations.github.models.GithubUser;
import io.levelops.integrations.gitlab.models.GitlabCommit;
import io.levelops.integrations.gitlab.models.GitlabEvent;
import io.levelops.integrations.gitlab.models.GitlabMergeRequest;
import io.levelops.integrations.gitlab.models.GitlabUser;
import io.levelops.integrations.helix_swarm.models.HelixSwarmReview;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("unused")
public class DbScmPullRequest {

    private static final Pattern DEPOT_PATTERN = Pattern.compile("//(.*)/.*");
    private static final ObjectNode EMPTY_JSON = JsonNodeFactory.instance.objectNode();
    private static final String REVIEW = "review";
    public static final String UNKNOWN = "_UNKNOWN_";
    public static final String REVIEWED = "REVIEWED";
    public static final String APPROVED = "APPROVED";
    public static final String UNAPPROVED = "UNAPPROVED";
    public static final String DECLINED = "DECLINED";
    private static final String NEEDS_WORK = "NEEDS WORK";
    public static final String COMMENTED = "COMMENTED";
    public static final String RESCOPED = "RESCOPED";
    public static final String MERGED = "MERGED";
    private static final String HELIX_SWARM_UNKNOWN_BRANCH = "unknown";
    private static final String COMMENT_SUFFIX = "-comment";
    public static final boolean SEPARATE_APPROVAL_AND_COMMENT = true;
    public static final boolean DO_NOT_SEPARATE_APPROVAL_AND_COMMENT = false;
    public static final String FORWARD_SLASH = "/";
    public static final String PR_LINK = "pr_link";
    public static final String NULL_PR_LINK = "#";
    public static final String HTTPS_GITHUB_COM = "https://github.com/";
    public static final String GITHUB_PULL = "/pull/";
    public static final String HTTPS_GITLAB_COM = "https://gitlab.com/";
    public static final String GITLAB_MR = "/-/merge_requests/";

    @JsonProperty("id")
    @JsonAlias({"pr_id"})
    private String id;

    @JsonProperty("repo_id")
    @JsonAlias({"pr_repo_id"})
    private List<String> repoIds;

    @JsonProperty("project")
    @JsonAlias({"pr_project"})
    private String project;

    @JsonProperty("number")
    @JsonAlias({"pr_number"})
    private String number;

    @JsonProperty("pr_link")
    private String prLink;

    @JsonProperty("metadata")
    @JsonAlias({"pr_metadata"})
    private Map<String, Object> metadata;

    @JsonProperty("integration_id")
    @JsonAlias({"pr_integration_id"})
    private String integrationId;

    @JsonProperty("creator")
    @JsonAlias({"pr_creator"})
    private String creator;

    @JsonProperty("merge_sha")
    @JsonAlias({"pr_merge_sha"})
    private String mergeSha;

    @JsonProperty("title")
    @JsonAlias({"pr_title"})
    private String title;

    @JsonProperty("source_branch")
    @JsonAlias({"pr_source_branch"})
    private String sourceBranch;

    @JsonProperty("target_branch")
    @JsonAlias({"pr_target_branch"})
    private String targetBranch;

    @JsonProperty("state")
    @JsonAlias({"pr_state"})
    private String state;

    @JsonProperty("merged")
    @JsonAlias({"pr_merged"})
    private Boolean merged;

    @JsonProperty("lines_added")
    @JsonAlias({"pr_lines_added"})
    private String linesAdded;

    @JsonProperty("lines_deleted")
    @JsonAlias({"pr_lines_deleted"})
    private String linesDeleted;

    @JsonProperty("lines_changed")
    @JsonAlias({"pr_lines_changed"})
    private String linesChanged;

    @JsonProperty("files_changed")
    @JsonAlias({"pr_files_changed"})
    private String filesChanged;

    @JsonProperty("code_change")
    @JsonAlias({"pr_code_change"})
    private String codeChange;

    @JsonProperty("comment_density")
    @JsonAlias({"pr_comment_density"})
    private String commentDensity;

    @JsonProperty("has_issue_keys")
    @JsonAlias({"pr_has_issue_keys"})
    private Boolean hasIssueKeys;

    @JsonProperty("review_type")
    @JsonAlias({"pr_review_type"})
    private String reviewType;

    @JsonProperty("collab_state")
    @JsonAlias({"pr_collab_state"})
    private String collabState;

    @JsonProperty("assignees")
    @JsonAlias({"pr_assignees"})
    private List<String> assignees;

    @JsonProperty("assignee_ids")
    @JsonAlias({"pr_assignee_ids"})
    private List<String> assigneeIds;

    @JsonProperty("assignees_info")
    @JsonAlias({"pr_assignees_info"})
    private List<DbScmUser> assigneesInfo;

    @JsonProperty("reviewers")
    @JsonAlias({"pr_reviewers"})
    private List<String> reviewers;

    @JsonProperty("reviewer_ids")
    @JsonAlias({"pr_reviewer_ids"})
    private List<String> reviewerIds;

    @JsonProperty("commenters")
    @JsonAlias({"pr_commenters"})
    private List<String> commenters;

    @JsonProperty("commenter_ids")
    @JsonAlias({"pr_commenter_ids"})
    private List<String> commenterIds;

    @JsonProperty("approver_ids")
    @JsonAlias({"pr_approver_ids"})
    private List<String> approverIds;

    @JsonProperty("approvers")
    @JsonAlias({"pr_approvers"})
    private List<String> approvers;

    @JsonProperty("labels")
    @JsonAlias({"pr_label"})
    private List<String> labels;

    @JsonProperty("pr_labels")
    @JsonAlias({"pr_pr_labels"})
    private List<DbScmPRLabel> prLabels;

    @JsonProperty("commit_shas")
    @JsonAlias({"pr_commit_shas"})
    private List<String> commitShas;

    @JsonProperty("reviews")
    @JsonAlias({"pr_reviews"})
    private List<DbScmReview> reviews;

    @JsonProperty("pr_updated_at")
    @JsonAlias({"pr_pr_updated_at"})
    private Long prUpdatedAt;

    @JsonProperty("pr_merged_at")
    @JsonAlias({"pr_pr_merged_at"})
    private Long prMergedAt;

    @JsonProperty("pr_closed_at")
    @JsonAlias({"pr_pr_closed_at"})
    private Long prClosedAt;

    @JsonProperty("pr_created_at")
    @JsonAlias({"pr_pr_created_at"})
    private Long prCreatedAt;

    @JsonProperty("created_at")
    @JsonAlias({"pr_createdat"})
    private Long createdAt;

    @JsonProperty("avg_author_response_time")
    @JsonAlias({"pr_avg_author_response_time"})
    private Long avgAuthorResponseTime;

    @JsonProperty("avg_reviewer_response_time")
    @JsonAlias({"pr_avg_reviewer_response_time"})
    private Long avgReviewerResponseTime;

    @JsonProperty("comment_time")
    @JsonAlias({"pr_comment_time"})
    private Long commentTime;

    @JsonProperty("approval_time")
    @JsonAlias({"pr_approval_time"})
    private Long approvalTime;

    @JsonProperty("creator_info")
    @JsonAlias({"pr_creator_info"})
    private DbScmUser creatorInfo; //contains DbScmUser object for creator

    @JsonProperty("creator_id")
    @JsonAlias({"pr_creator_id"})
    private String creatorId; //refers to unique row in scm_users

    @JsonProperty("workitem_ids")
    @JsonAlias({"pr_workitem_ids"})
    List<String> workitemIds;

    @JsonProperty("issue_keys")
    @JsonAlias({"pr_issue_keys"})
    private List<String> issueKeys;

    @JsonProperty("reviewers_info")
    @JsonAlias({"pr_reviewers_info"})
    private List<DbScmReview> reviewerInfo;

    @JsonProperty("commenters_info")
    @JsonAlias({"pr_commenters_info"})
    private List<DbScmReview> commenterInfo;

    @JsonProperty("approvers_info")
    @JsonAlias({"pr_approvers_info"})
    private List<DbScmReview> approverInfo;

    @JsonProperty("created_day")
    @JsonAlias({"pr_created_day"})
    private String createdDay;

    @JsonProperty("merged_day")
    @JsonAlias({"pr_merged_day"})
    private String mergedDay;

    @JsonProperty("closed_day")
    @JsonAlias({"pr_closed_day"})
    private String closedDay;

    @JsonProperty("commits_stat")
    @JsonAlias({"pr_commits_stat"})
    private List<DbScmCommitStats> commitStats;

    @JsonProperty("additions")
    @JsonAlias({"pr_additions"})
    private Integer additions;

    @JsonProperty("deletions")
    @JsonAlias({"pr_deletions"})
    private Integer deletions;

    @JsonProperty("changes")
    @JsonAlias({"pr_changes"})
    private Integer change;

    @JsonProperty("files_count")
    @JsonAlias({"pr_files_count"})
    private Integer filesCount;

    @JsonProperty("comment_count")
    @JsonAlias({"pr_comment_count"})
    private Integer commentCount;

    @JsonProperty("reviewer_count")
    @JsonAlias({"pr_reviewer_count"})
    private Integer reviewerCount;

    @JsonProperty("approver_count")
    @JsonAlias({"pr_approver_count"})
    private Integer approverCount;

    @JsonProperty("approval_status")
    @JsonAlias({"pr_approval_status"})
    private String approvalStatus;

    @JsonProperty("technology")
    @JsonAlias({"pr_technology"})
    private List<String> technology;

    @JsonProperty("first_committed_at")
    @JsonAlias({"pr_first_committed_at"})
    private Long firstCommittedAt;

    @JsonProperty("cycle_time")
    @JsonAlias({"pr_cycle_time"})
    private Long cycleTime;

    @JsonProperty("review_cycle_time")
    @JsonAlias({"pr_review_cycle_time"})
    private Long reviewCycleTime;

    @JsonProperty("review_merge_cycle_time")
    @JsonAlias({"pr_review_merge_cycle_time"})
    private Long reviewMergeCycleTime;

    @JsonProperty("loc")
    @JsonAlias({"pr_loc"})
    private Integer loc;

    private static List<DbScmReview> parserScmReviewsFromGithubPR(List<GithubReview> githubPRReviews, String integrationId) {
        List<DbScmReview> scmReviews = new ArrayList<>();
        for(GithubReview review : CollectionUtils.emptyIfNull(githubPRReviews)) {
            DbScmReview scmReview = DbScmReview.builder()
                    .reviewerInfo(DbScmUser.fromGithubPullRequestReviewer(review, integrationId))
                    .reviewId(review.getId())
                    .reviewer(review.getUser().getLogin())
                    .reviewedAt(review.getSubmitted_at().toInstant().getEpochSecond())
                    .state(review.getState())
                    .build();
            scmReviews.add(scmReview);

            boolean reviewContainsComment = StringUtils.isNotBlank(review.getBody());
            boolean currentReviewIsNotComment = ! COMMENTED.equals(review.getState());
            boolean addSecondReviewForComment = reviewContainsComment && currentReviewIsNotComment;
            if (addSecondReviewForComment) {
                DbScmReview scmSecondReview = DbScmReview.builder()
                        .reviewerInfo(DbScmUser.fromGithubPullRequestReviewer(review, integrationId))
                        .reviewId(review.getId() + COMMENT_SUFFIX)
                        .reviewer(review.getUser().getLogin())
                        .reviewedAt(review.getSubmitted_at().toInstant().getEpochSecond())
                        .state(COMMENTED)
                        .build();
                scmReviews.add(scmSecondReview);
            }
        }
        return scmReviews;
    }

    public static DbScmPullRequest fromGithubPullRequest(GithubPullRequest source,
                                                         String repoId,
                                                         String integrationId, Integration integration) {
        return fromGithubPullRequest(source, repoId, integrationId, DO_NOT_SEPARATE_APPROVAL_AND_COMMENT, integration);
    }
    public static DbScmPullRequest fromGithubPullRequest(GithubPullRequest source,
                                                         String repoId,
                                                         String integrationId, boolean shouldSeparateApprovalAndComment, Integration integration) {
        List<DbScmReview> reviews = List.of();
        if (shouldSeparateApprovalAndComment) {
            reviews = parserScmReviewsFromGithubPR(source.getReviews(), integrationId);
        } else {
            reviews = IterableUtils.parseIterable(
                    source.getReviews(),
                    review -> DbScmReview.builder()
                            .reviewerInfo(DbScmUser.fromGithubPullRequestReviewer(review, integrationId))
                            .reviewId(review.getId())
                            .reviewer(review.getUser().getLogin())
                            .reviewedAt(review.getSubmitted_at().toInstant().getEpochSecond())
                            .state(review.getState())
                            .build());
        }
        List<GithubUser> assignees = source.getAssignees();
        List<DbScmUser> assigneesInfo = assignees.stream()
                .map(assignee -> DbScmUser.builder()
                        .cloudId(assignee.getLogin())
                        .displayName(assignee.getLogin())
                        .originalDisplayName(assignee.getLogin())
                        .integrationId(integrationId).build())
                .collect(Collectors.toList());

        List<String> commitShas = IterableUtils.parseIterable(
                source.getCommits(),
                GithubCommit::getSha);
        // LEV-3954: Need to update the commit_shas column after changes made for LEV-3564
        // ToDo: Need to remove after some time...
        if (source.getOlderCommits() != null && source.getOlderCommits() instanceof List) {
            ObjectMapper objectMapper = DefaultObjectMapper.get();
            List<GithubCommit> commits = objectMapper.convertValue(source.getOlderCommits(), objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, GithubCommit.class));
            List<String> olderCommitsShas = IterableUtils.parseIterable(commits, GithubCommit::getSha);
            Set<String> allCommitShas = new HashSet<>();
            allCommitShas.addAll(olderCommitsShas);
            allCommitShas.addAll(commitShas);
            commitShas.clear();
            commitShas = new ArrayList<>(allCommitShas);
        }
        // endregion
        List<DbScmPRLabel> prLabels = CollectionUtils.emptyIfNull(source.getLabels()).stream()
                .map(l -> DbScmPRLabel.builder().cloudId(l.getId()).name(l.getName().toLowerCase()).description(l.getDescription()).labelAddedAt(source.getUpdatedAt().toInstant()).build())
                .collect(Collectors.toList());
        String sourceBranchName = "";
        String targetBranchName = "";
        if (source.getHead() != null) {
            sourceBranchName = source.getHead().getRef();
        }
        if (source.getBase() != null) {
            targetBranchName = source.getBase().getRef();
        }

        Map<String, Object> metadata = new HashMap<>();
        GithubPullRequest.Ref githubPrRef = source.getBase();
        if (githubPrRef != null) {
            GithubPullRequest.RepoId githubRepo = githubPrRef.getRepo();
            if (githubRepo != null) {
                String prLink = githubRepo.getFullName();
                String baseUrl = (integration != null && integration.getSatellite()) ? sanitizeUrl(integration.getUrl()) : HTTPS_GITHUB_COM;
                metadata.put(PR_LINK, prLink == null ? NULL_PR_LINK : baseUrl + prLink + GITHUB_PULL + source.getNumber());
            }
        }
        return DbScmPullRequest.builder()
                .state(source.getState())
                .title(MoreObjects.firstNonNull(source.getTitle(), ""))
                .number(String.valueOf(source.getNumber()))
                .workitemIds(ScmIssueCorrelationUtil.extractWorkitems(source.getTitle(), sourceBranchName))
                .issueKeys(ScmIssueCorrelationUtil.extractJiraKeys(source.getTitle(), sourceBranchName))
                .integrationId(integrationId)
                .assignees(IterableUtils.parseIterable(assignees,
                        GithubUser::getLogin))
                .assigneesInfo(assigneesInfo)
                .labels(IterableUtils.parseIterable(source.getLabels(), label -> label.getName().toLowerCase()))
                .prLabels(prLabels)
                .reviews(reviews)
                .sourceBranch(sourceBranchName)
                .targetBranch(targetBranchName)
                .creator(source.getUser().getLogin())
                .creatorInfo(DbScmUser.fromGithubPullRequestCreator(source, integrationId))
                .mergeSha(source.getMergeCommitSha())
                .repoIds(List.of(repoId))
                .project(MoreObjects.firstNonNull(repoId, ""))
                .commitShas(commitShas)
                .merged(source.getMergedAt() != null)
                .prCreatedAt(DateUtils.toEpochSecond(source.getCreatedAt()))
                .prUpdatedAt(DateUtils.toEpochSecond(source.getUpdatedAt()))
                .prMergedAt(DateUtils.toEpochSecond(source.getMergedAt()))
                .prClosedAt(DateUtils.toEpochSecond(source.getClosedAt()))
                .metadata(metadata)
                .build();
    }

    private static String sanitizeUrl(String url) {
        if(url == null){
            return StringUtils.EMPTY;
        }
        if (!url.startsWith("http")) {
            url= "https://"+url;
        }
        if(!url.endsWith("/")){
            url= url+"/";
        }
        return url;
    }

    @Value
    @Builder(toBuilder = true)
    private static class PRMergeInfo {
        Boolean merged;
        String mergeSha;
        Long prMergedAt;
    }

    /**
     * If pr is null or pr is open or pr doesn't contain any activities with action as MERGED
     * then the pr is not merged yet.
     * Else the pr is merged at MERGED activity createdDate.
     */
    private static PRMergeInfo extractMergeInfoFromBitbucketServerPR(BitbucketServerPullRequest source) {
        if (source == null || source.getOpen() || source.getActivities() == null) {
            return PRMergeInfo.builder()
                    .merged(false)
                    .build();
        }
        BitbucketServerPRActivity activity = source.getActivities().stream()
                .filter(prActivity -> StringUtils.equalsIgnoreCase(prActivity.getAction(), MERGED))
                .findFirst()
                .orElse(null);
        if (activity == null) {
            return PRMergeInfo.builder()
                    .merged(false)
                    .build();
        }
        return PRMergeInfo.builder()
                .merged(true)
                .mergeSha(Optional.ofNullable(activity.getCommit())
                        .map(BitbucketServerPRActivity.Commit::getId).orElse(null))
                .prMergedAt(activity.getCreatedDate())
                .build();
    }

    private static List<DbScmReview> extractReviewsFromBitbucketServerPR(BitbucketServerPullRequest source, String integrationId) {
        List<String> prReviewActivities = List.of(REVIEWED, APPROVED, UNAPPROVED, DECLINED, NEEDS_WORK, COMMENTED, RESCOPED);
        List<BitbucketServerPRActivity> reviewed = Optional.ofNullable(source.getActivities())
                .orElseGet(Collections::emptyList).stream()
                .filter(bitbucketServerPRActivity -> prReviewActivities.contains(bitbucketServerPRActivity.getAction()))
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(reviewed)) {
            return reviewed.stream()
                    .map(activity -> DbScmReview.builder()
                            .reviewerInfo(DbScmUser.fromBitbucketServerPullRequestReviewer(activity, integrationId))
                            .reviewer(activity.getUser().getDisplayName())
                            .reviewId(String.valueOf(activity.getId()))
                            .state(Optional.ofNullable(activity.getAction())
                                    .orElse(UNKNOWN))
                            .reviewedAt(TimeUnit.MILLISECONDS.toSeconds(activity.getCreatedDate()))
                            .build()).collect(Collectors.toList());

        } else {
            return Collections.emptyList();
        }
    }

    public static DbScmPullRequest fromBitbucketServerPullRequest(BitbucketServerPullRequest source,
                                                                  String repoId,
                                                                  String integrationId) {
        PRMergeInfo mergeInfo = extractMergeInfoFromBitbucketServerPR(source);
        List<DbScmUser> assigneesInfo = source.getParticipants().stream()
                .filter(participant -> participant.getRole() != null && participant.getRole().equals("PARTICIPANT"))
                .map(participant -> DbScmUser.builder()
                        .integrationId(integrationId)
                        .cloudId(Optional.ofNullable(String.valueOf(participant.getUser().getId())).orElse(UNKNOWN))
                        .displayName(Optional.of(participant.getUser()).map(BitbucketServerUser::getDisplayName).orElse(UNKNOWN))
                        .originalDisplayName(Optional.of(participant.getUser()).map(BitbucketServerUser::getDisplayName).orElse(UNKNOWN))
                        .build())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return DbScmPullRequest.builder()
                .repoIds(List.of(repoId))
                .workitemIds(ScmIssueCorrelationUtil.extractWorkitems(source.getTitle(), source.getFromRef().getDisplayId()))
                .issueKeys(ScmIssueCorrelationUtil.extractJiraKeys(source.getTitle(), source.getFromRef().getDisplayId()))
                .project(Optional.ofNullable(source.getFromRef())
                        .map(BitbucketServerPullRequest.RepoRef::getRepository)
                        .map(BitbucketServerRepository::getProject)
                        .map(BitbucketServerProject::getName).orElse(UNKNOWN))
                .number(MoreObjects.firstNonNull(String.valueOf(source.getId()), UNKNOWN))
                .integrationId(integrationId)
                .creator(Optional.ofNullable(source.getAuthor())
                        .map(BitbucketServerPullRequest.PRUser::getUser)
                        .map(BitbucketServerUser::getDisplayName).orElse(UNKNOWN))
                .creatorInfo(DbScmUser.fromBitbucketServerPullRequestCreator(source, integrationId))
                .title(MoreObjects.firstNonNull(source.getTitle(), UNKNOWN))
                .sourceBranch(Optional.ofNullable(source.getFromRef())
                        .map(BitbucketServerPullRequest.RepoRef::getDisplayId).orElse(UNKNOWN))
                .targetBranch(Optional.ofNullable(source.getToRef())
                        .map(BitbucketServerPullRequest.RepoRef::getDisplayId).orElse(UNKNOWN))
                .state(MoreObjects.firstNonNull(source.getState(), UNKNOWN))
                .assignees(List.copyOf(source.getParticipants().stream()
                        .filter(participant -> participant.getRole() != null && participant.getRole().equals("PARTICIPANT"))
                        .map(participant -> ObjectUtils.firstNonNull(
                                participant.getUser().getDisplayName(),
                                participant.getUser().getName()))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet())))
                .assigneesInfo(assigneesInfo)
                .commitShas(Optional.ofNullable(source.getPrCommits())
                        .orElseGet(Collections::emptyList).stream()
                        .map(BitbucketServerCommit::getId)
                        .collect(Collectors.toList()))
                .reviews(extractReviewsFromBitbucketServerPR(source, integrationId))
                .merged(mergeInfo.getMerged())
                .mergeSha(mergeInfo.getMergeSha())
                .prMergedAt(DateUtils.toEpochSecond(mergeInfo.getPrMergedAt()))
                .prUpdatedAt(DateUtils.toEpochSecond(source.getUpdatedDate()))
                .prClosedAt(source.getState().equalsIgnoreCase("open")
                        ? null : DateUtils.toEpochSecond(source.getUpdatedDate()))
                .prCreatedAt(DateUtils.toEpochSecond(source.getCreatedDate()))
                .labels(List.of())
                .build();
    }

    public static DbScmPullRequest fromGitlabMergeRequest(GitlabMergeRequest source,
                                                          String projectId,
                                                          String integrationId, Integration integration) {

        List<DbScmReview> reviews = parseGitlabReviews(source, integrationId);

        Set<String> assigneeLogins = new HashSet<>();
        List<DbScmUser> assigneesInfo = new ArrayList<>();
        parseGitlabUser(source.getAuthor(), integrationId, assigneeLogins, assigneesInfo); // TODO  not sure if author should by default be an assignee?
        parseGitlabUser(source.getClosedBy(), integrationId, assigneeLogins, assigneesInfo);
        parseGitlabUser(source.getMergedBy(), integrationId, assigneeLogins, assigneesInfo);
        ListUtils.emptyIfNull(source.getAssignees()).forEach(u -> parseGitlabUser(u, integrationId, assigneeLogins, assigneesInfo));

        String authorLogin = Optional.ofNullable(source.getAuthor()).map(GitlabUser::getUsername).filter(StringUtils::isNotBlank).orElse(UNKNOWN);

        Map<String, Object> metadata = new HashMap<>();
        if (projectId != null) {
            String prLink = projectId;
            String baseUrl = (integration != null && integration.getSatellite()) ? sanitizeUrl(integration.getUrl()) : HTTPS_GITLAB_COM;
            metadata.put(PR_LINK, baseUrl + prLink + GITLAB_MR + source.getIid());
        }
        return DbScmPullRequest.builder()
                .id(source.getIid())
                .repoIds(List.of(projectId))
                .project(MoreObjects.firstNonNull(projectId, ""))
                .state(source.getState())
                .workitemIds(ScmIssueCorrelationUtil.extractWorkitems(source.getTitle(), source.getSourceBranch()))
                .issueKeys(ScmIssueCorrelationUtil.extractJiraKeys(source.getTitle(), source.getSourceBranch()))
                .integrationId(integrationId)
                .number(source.getIid())
                .title(MoreObjects.firstNonNull(source.getTitle(), ""))
                .creator(authorLogin)
                .creatorInfo(DbScmUser.fromGitlabPullRequestCreator(source, integrationId))
                .mergeSha(source.getSha())
                .sourceBranch(source.getSourceBranch())
                .targetBranch(source.getTargetBranch())
                .merged(source.getMergedAt() != null)
                .assignees(new ArrayList<>(assigneeLogins))
                .assigneesInfo(assigneesInfo)
                .labels(source.getLabels())
                .commitShas(ListUtils.emptyIfNull(source.getCommits()).stream().map(GitlabCommit::getId).collect(Collectors.toList()))
                .prUpdatedAt(source.getUpdatedAt().toInstant().getEpochSecond())
                .prMergedAt(DateUtils.toEpochSecond(source.getMergedAt()))
                .prCreatedAt(DateUtils.toEpochSecond(source.getCreatedAt()))
                .prClosedAt(DateUtils.toEpochSecond(source.getClosedAt()))
                .reviews(reviews)
                .metadata(metadata)
                .build();

    }

    private static void parseGitlabUser(GitlabUser user, String integrationId, Set<String> assigneeLogins, List<DbScmUser> assigneesInfo) {
        if (user == null) {
            return;
        }
        String displayName = StringUtils.defaultIfBlank(user.getName(), UNKNOWN);
        String login = StringUtils.defaultIfBlank(user.getUsername(), UNKNOWN);
        if (assigneeLogins.contains(login)) {
            return;
        }

        assigneeLogins.add(login);
        assigneesInfo.add(DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(login)
                .displayName(displayName)
                .originalDisplayName(displayName)
                .build());
    }

    public static List<DbScmReview> parseGitlabReviews(GitlabMergeRequest source,
                                                       String integrationId) {
        return ListUtils.emptyIfNull(source.getMergeRequestEvents()).stream()
                .filter(Objects::nonNull)
                .filter(event -> event.getTargetId() != null && event.getProjectId() != null)
                .filter(event -> source.getId().equalsIgnoreCase(event.getTargetId()) && source.getProjectId() == Long.parseLong(event.getProjectId()))
                .filter(event -> GitlabEvent.GITLAB_MR_EVENTS_TO_SCM_MAPPINGS.containsKey(event.getActionName()))
                .map(mrEvent -> DbScmReview.builder()
                        .reviewerInfo(DbScmUser.fromGitlabPullRequestReviewer(mrEvent, integrationId))
                        .reviewId(mrEvent.getId())
                        .reviewer(mrEvent.getAuthorUsername()) // login not display name
                        .prId(source.getIid())
                        .state(parseGitlabPrState(mrEvent))
                        .reviewedAt(DateUtils.toEpochSecond(mrEvent.getCreatedAt()))
                        .build()
                ).collect(Collectors.toList());
    }

    public static String parseGitlabPrState(GitlabEvent mrEvent) {
        if (StringUtils.isBlank(mrEvent.getActionName())) {
            return "UNKNOWN";
        }
        return GitlabEvent.GITLAB_MR_EVENTS_TO_SCM_MAPPINGS.getOrDefault(
                mrEvent.getActionName().toLowerCase(),
                mrEvent.getActionName().toUpperCase());
    }

    public static DbScmPullRequest fromHelixSwarmReview(HelixSwarmReview review,
                                                        Set<String> repoIds,
                                                        String integrationId) {
        /*
        String depotName = ListUtils.emptyIfNull(review.getVersions()).stream()
                .findFirst()
                .map(HelixSwarmChange::getStream)
                .map(stream -> {
                    Matcher matcher = DEPOT_PATTERN.matcher(stream);
                    if (matcher.matches()) {
                        return matcher.group(1);
                    }
                    return null;
                })
                .orElse(UNKNOWN);
        String branch = ListUtils.emptyIfNull(review.getVersions()).stream()
                .findFirst().map(HelixSwarmChange::getStream).orElse(UNKNOWN);

         */
        Long prCreatedAt = review.getCreatedAt();
        Instant prUpdatedAt = HelixSwarmReview.parseReviewUpdateDate(review.getUpdatedAt());
        ArrayList<String> sortedRepoIdList = new ArrayList<>(repoIds);
        Collections.sort(sortedRepoIdList);
        return DbScmPullRequest.builder()
                .integrationId(integrationId)
                .repoIds(sortedRepoIdList)
                .workitemIds(ScmIssueCorrelationUtil.extractWorkitems(review.getDescription()))
                .issueKeys(ScmIssueCorrelationUtil.extractJiraKeys(review.getDescription()))
                .project(sortedRepoIdList.stream().findFirst().orElse(""))
                .state(Optional.ofNullable(review.getState())
                        .map(state -> CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, state)).orElse(UNKNOWN))
                .number(String.valueOf(review.getId()))
                .creator(review.getAuthor())
                .creatorInfo(DbScmUser.fromHelixSwarmReviewCreator(review, integrationId))
                .sourceBranch(HELIX_SWARM_UNKNOWN_BRANCH)
                .targetBranch(HELIX_SWARM_UNKNOWN_BRANCH)
                .merged(APPROVED.equalsIgnoreCase(review.getState()))
                .assignees(IteratorUtils.toList(MoreObjects.firstNonNull(review.getParticipants(), EMPTY_JSON).fieldNames()))
                .labels(List.of())
                .reviews(ListUtils.emptyIfNull(review.getReviews()).stream()
                        .filter(activity -> activity.getUser() != null && REVIEW.equalsIgnoreCase(activity.getType()))
                        .map(activity -> DbScmReview.builder()
                                .reviewerInfo(DbScmUser.fromHelixSwarmReviewReviewer(activity, integrationId))
                                .reviewer(activity.getUser())
                                .reviewId(String.valueOf(activity.getId()))
                                .state(Optional.ofNullable(activity.getAction())
                                        .map(str -> str.replace(" ", "_").toUpperCase())
                                        .orElse(UNKNOWN))
                                .reviewedAt(activity.getTime())
                                .build())
                        .collect(Collectors.toList()))
                .commitShas(ListUtils.emptyIfNull(review.getCommits()).stream()
                        .map(String::valueOf)
                        .collect(Collectors.toList()))
                .title(review.getDescription())
                .prUpdatedAt((prUpdatedAt != null) ? prUpdatedAt.getEpochSecond() : prCreatedAt)
                .prCreatedAt(prCreatedAt)
                .prMergedAt(APPROVED.equalsIgnoreCase(review.getState()) ?
                        ((prUpdatedAt != null) ? prUpdatedAt.getEpochSecond() : prCreatedAt) :
                        null)
                .build();
    }
}
