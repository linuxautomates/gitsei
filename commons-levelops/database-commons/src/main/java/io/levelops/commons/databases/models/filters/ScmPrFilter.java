package io.levelops.commons.databases.models.filters;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.caching.CacheHashUtils;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.web.exceptions.BadRequestException;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getExcludedRange;
import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getListOrDefault;
import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getNumericRange;
import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getTimeRange;
import static io.levelops.commons.caching.CacheHashUtils.hashDataMapOfStrings;

@Log4j2
@Value
@Builder(toBuilder = true)
public class ScmPrFilter {
    DISTINCT across;
    CALCULATION calculation;
    List<String> repoIds;
    List<String> projects;
    List<String> creators;
    List<String> sourceBranches;
    List<String> targetBranches;
    List<String> reviewers;
    List<String> titles;
    List<String> approvers;
    List<String> commenters;
    List<String> reviewerIds;
    List<String> reviewTypes;
    List<String> states;
    List<String> labels;
    List<String> assignees;
    List<String> integrationIds;
    List<String> codeChanges;
    List<String> approvalStatuses;
    List<String> collabStates;
    List<String> commentDensities;
    List<String> prCreatedDaysOfWeek;
    List<String> prMergedDaysOfWeek;
    List<String> prClosedDaysOfWeek;
    List<String> commitTitles;
    String hasIssueKeys;
    Boolean hasComments;
    Map<String, Boolean> missingFields;
    ImmutablePair<Long, Long> locRange;
    ImmutablePair<Long, Long> approverCount;
    ImmutablePair<Long, Long> reviewerCount;
    ImmutablePair<Long, Long> prCreatedRange;
    ImmutablePair<Long, Long> prMergedRange;
    ImmutablePair<Long, Long> prClosedRange;
    ImmutablePair<Long, Long> prUpdatedRange;
    ImmutablePair<Long, Long> prReviewedRange;
    Map<String, Map<String, String>> partialMatch;
    Map<String, Map<String, String>> excludePartialMatch;
    Map<String, SortingOrder> sort;
    Set<UUID> orgProductIds;
    AGG_INTERVAL aggInterval;

    List<UUID> ids;

    //config
    String codeChangeUnit;
    Map<String, String> codeChangeSizeConfig;
    Map<String, String> commentDensitySizeConfig;

    List<String> excludeRepoIds;
    List<String> excludeProjects;
    List<String> excludeCreators;
    List<String> excludeReviewTypes;
    List<String> excludeSourceBranches;
    List<String> excludeTargetBranches;
    List<String> excludeReviewers;
    List<String> excludeTitles;
    List<String> excludeStates;
    List<String> excludeLabels;
    List<String> excludeApprovers;
    List<String> excludeCommenters;
    List<String> excludeAssignees;
    List<String> excludeCodeChanges;
    List<String> excludeCommentDensities;
    List<String> excludeApprovalStatuses;
    List<String> excludeCollabStates;
    List<String> excludeCommitTitles;
    ImmutablePair<Long, Long> excludeLocRange;
    Boolean isApplyOuOnVelocityReport;

    public enum DISTINCT {
        repo_id,
        project,
        branch,
        source_branch,
        target_branch,
        reviewer,
        commenter,
        comment_density,
        approver,
        approval_status,
        review_type,
        assignee,
        creator,
        approver_count,
        reviewer_count,
        collab_state,
        state,
        code_change,
        label,
        technology,
        none,
        //these are time based
        pr_created,
        pr_closed,
        pr_updated,
        pr_merged,
        pr_reviewed;

        public static DISTINCT fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(DISTINCT.class, st);
        }
    }

    public enum CALCULATION {
        first_review_time,
        merge_time,
        first_review_to_merge_time,
        author_response_time,
        reviewer_response_time,
        reviewer_comment_time,
        reviewer_approve_time,
        count, // just a count of rows
        //DORA metrics
        deployment_frequency,
        failure_rate,
        lead_time_for_changes,
        mean_time_to_recover;

        public static CALCULATION fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(CALCULATION.class, st);
        }
    }

    public enum MISSING_BUILTIN_FIELD {
        pr_closed, pr_merged;

        public static ScmPrFilter.MISSING_BUILTIN_FIELD fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(ScmPrFilter.MISSING_BUILTIN_FIELD.class, st);
        }
    }

    public String generateCacheRawString() {
        StringBuilder dataToHash = new StringBuilder();
        if (across != null)
            dataToHash.append("across=").append(across);
        if (calculation != null)
            dataToHash.append(",calculation=").append(calculation);
        if (codeChangeUnit != null)
            dataToHash.append(",codeChangeUnit=").append(codeChangeUnit);
        if (hasIssueKeys != null)
            dataToHash.append(",hasIssueKeys=").append(hasIssueKeys);
        hashDataMapOfStrings(dataToHash, "missingFields", missingFields);
        if (CollectionUtils.isNotEmpty(repoIds)) {
            ArrayList<String> tempList = new ArrayList<>(repoIds);
            Collections.sort(tempList);
            dataToHash.append(",repoIds=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(orgProductIds)) {
            ArrayList<String> tempList = orgProductIds.stream().sorted().map(UUID::toString)
                    .collect(Collectors.toCollection(ArrayList::new));
            dataToHash.append(",ordProductIds=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(ids)) {
            ArrayList<String> tempList = ids.stream().sorted().map(UUID::toString)
                    .collect(Collectors.toCollection(ArrayList::new));
            dataToHash.append(",ids=").append(String.join(",", tempList));
        }
        if (hasComments != null) {
            dataToHash.append(",hasComments=").append(hasComments);
        }
        if (CollectionUtils.isNotEmpty(projects)) {
            ArrayList<String> tempList = new ArrayList<>(projects);
            Collections.sort(tempList);
            dataToHash.append(",projects=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(commentDensities)) {
            ArrayList<String> tempList = new ArrayList<>(commentDensities);
            Collections.sort(tempList);
            dataToHash.append(",commentDensities=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(approvalStatuses)) {
            ArrayList<String> tempList = new ArrayList<>(approvalStatuses);
            Collections.sort(tempList);
            dataToHash.append(",approvalStatuses=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(collabStates)) {
            ArrayList<String> tempList = new ArrayList<>(collabStates);
            Collections.sort(tempList);
            dataToHash.append(",collabStates=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(excludeCollabStates)) {
            ArrayList<String> tempList = new ArrayList<>(excludeCollabStates);
            Collections.sort(tempList);
            dataToHash.append(",excludeCollabStates=").append(String.join(",", tempList));
        }
        if (aggInterval != null)
            dataToHash.append(",aggInterval=").append(aggInterval);
        if (CollectionUtils.isNotEmpty(creators)) {
            ArrayList<String> tempList = new ArrayList<>(creators);
            Collections.sort(tempList);
            dataToHash.append(",creators=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(sourceBranches)) {
            ArrayList<String> tempList = new ArrayList<>(sourceBranches);
            Collections.sort(tempList);
            dataToHash.append(",sourceBranches=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(targetBranches)) {
            ArrayList<String> tempList = new ArrayList<>(targetBranches);
            Collections.sort(tempList);
            dataToHash.append(",targetBranches=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(codeChanges)) {
            ArrayList<String> tempList = new ArrayList<>(codeChanges);
            Collections.sort(tempList);
            dataToHash.append(",codeChanges=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(reviewers)) {
            ArrayList<String> tempList = new ArrayList<>(reviewers);
            Collections.sort(tempList);
            dataToHash.append(",reviewers=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(titles)) {
            ArrayList<String> tempList = new ArrayList<>(titles);
            Collections.sort(tempList);
            dataToHash.append(",titles=").append(String.join(",", tempList));
        }
        if (MapUtils.isNotEmpty(excludePartialMatch)) {
            TreeSet<String> fields = new TreeSet<>(excludePartialMatch.keySet());
            dataToHash.append(",excludePartialMatch=(");
            for (String field : fields) {
                Map<String, String> innerMap = excludePartialMatch.get(field);
                TreeSet<String> innerFields = new TreeSet<>(innerMap.keySet());
                dataToHash.append("(");
                for (String innerField : innerFields) {
                    dataToHash.append(innerField).append("=").append(innerMap.get(innerField)).append(",");
                }
                dataToHash.append("),");
            }
            dataToHash.append(")");
        }
        if (CollectionUtils.isNotEmpty(states)) {
            ArrayList<String> tempList = new ArrayList<>(states);
            Collections.sort(tempList);
            dataToHash.append(",states=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(labels)) {
            ArrayList<String> tempList = new ArrayList<>(labels);
            Collections.sort(tempList);
            dataToHash.append(",labels=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(commenters)) {
            ArrayList<String> tempList = new ArrayList<>(commenters);
            Collections.sort(tempList);
            dataToHash.append(",commenters=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(assignees)) {
            ArrayList<String> tempList = new ArrayList<>(assignees);
            Collections.sort(tempList);
            dataToHash.append(",assignees=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(approvers)) {
            ArrayList<String> tempList = new ArrayList<>(approvers);
            Collections.sort(tempList);
            dataToHash.append(",approvers=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(reviewTypes)) {
            ArrayList<String> tempList = new ArrayList<>(reviewTypes);
            Collections.sort(tempList);
            dataToHash.append(",reviewTypes=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            ArrayList<String> tempList = new ArrayList<>(integrationIds);
            Collections.sort(tempList);
            dataToHash.append(",integrationIds=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(commitTitles)) {
            ArrayList<String> tempList = new ArrayList<>(commitTitles);
            Collections.sort(tempList);
            dataToHash.append(",commitTitles=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(excludeRepoIds)) {
            ArrayList<String> tempList = new ArrayList<>(excludeRepoIds);
            Collections.sort(tempList);
            dataToHash.append(",excludeRepoIds=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(excludeReviewTypes)) {
            ArrayList<String> tempList = new ArrayList<>(excludeReviewTypes);
            Collections.sort(tempList);
            dataToHash.append(",excludeReviewTypes=").append(String.join(",", tempList));
        }
        if (prMergedRange != null) {
            dataToHash.append(",prMergedRange=");
            if (prMergedRange.getLeft() != null)
                dataToHash.append(prMergedRange.getLeft()).append("-");
            if (prMergedRange.getRight() != null)
                dataToHash.append(prMergedRange.getRight());
        }
        if (CollectionUtils.isNotEmpty(excludeProjects)) {
            ArrayList<String> tempList = new ArrayList<>(excludeProjects);
            Collections.sort(tempList);
            dataToHash.append(",excludeProjects=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(excludeCommentDensities)) {
            ArrayList<String> tempList = new ArrayList<>(excludeCommentDensities);
            Collections.sort(tempList);
            dataToHash.append(",excludeCommentDensities=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(excludeCodeChanges)) {
            ArrayList<String> tempList = new ArrayList<>(excludeCodeChanges);
            Collections.sort(tempList);
            dataToHash.append(",excludeCodeChanges=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(excludeCreators)) {
            ArrayList<String> tempList = new ArrayList<>(excludeCreators);
            Collections.sort(tempList);
            dataToHash.append(",excludeCreators=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(excludeSourceBranches)) {
            ArrayList<String> tempList = new ArrayList<>(excludeSourceBranches);
            Collections.sort(tempList);
            dataToHash.append(",excludeSourceBranches=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(excludeTargetBranches)) {
            ArrayList<String> tempList = new ArrayList<>(excludeTargetBranches);
            Collections.sort(tempList);
            dataToHash.append(",excludeTargetBranches=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(excludeApprovers)) {
            ArrayList<String> tempList = new ArrayList<>(excludeApprovers);
            Collections.sort(tempList);
            dataToHash.append(",excludeApprovers=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(excludeCommenters)) {
            ArrayList<String> tempList = new ArrayList<>(excludeCommenters);
            Collections.sort(tempList);
            dataToHash.append(",excludeCommenters=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(excludeApprovalStatuses)) {
            ArrayList<String> tempList = new ArrayList<>(excludeApprovalStatuses);
            Collections.sort(tempList);
            dataToHash.append(",excludeApprovalStatuses=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(excludeReviewers)) {
            ArrayList<String> tempList = new ArrayList<>(excludeReviewers);
            Collections.sort(tempList);
            dataToHash.append(",excludeReviewers=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(excludeStates)) {
            ArrayList<String> tempList = new ArrayList<>(excludeStates);
            Collections.sort(tempList);
            dataToHash.append(",excludeStates=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(excludeLabels)) {
            ArrayList<String> tempList = new ArrayList<>(excludeLabels);
            Collections.sort(tempList);
            dataToHash.append(",excludeLabels=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(excludeAssignees)) {
            ArrayList<String> tempList = new ArrayList<>(excludeAssignees);
            Collections.sort(tempList);
            dataToHash.append(",excludeAssignees=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(excludeCommitTitles)) {
            ArrayList<String> tempList = new ArrayList<>(excludeCommitTitles);
            Collections.sort(tempList);
            dataToHash.append(",excludeCommitTitles=").append(String.join(",", tempList));
        }
        if (prCreatedRange != null) {
            dataToHash.append(",prCreatedRange=");
            if (prCreatedRange.getLeft() != null)
                dataToHash.append(prCreatedRange.getLeft()).append("-");
            if (prCreatedRange.getRight() != null)
                dataToHash.append(prCreatedRange.getRight());
        }
        if (prClosedRange != null) {
            dataToHash.append(",prClosedRange=");
            if (prClosedRange.getLeft() != null)
                dataToHash.append(prClosedRange.getLeft()).append("-");
            if (prClosedRange.getRight() != null)
                dataToHash.append(prClosedRange.getRight());
        }
        if (prUpdatedRange != null) {
            dataToHash.append(",prUpdatedRange=");
            if (prUpdatedRange.getLeft() != null)
                dataToHash.append(prUpdatedRange.getLeft()).append("-");
            if (prUpdatedRange.getRight() != null)
                dataToHash.append(prUpdatedRange.getRight());
        }
        if (locRange != null) {
            dataToHash.append(",locRange=");
            if (locRange.getLeft() != null)
                dataToHash.append(locRange.getLeft()).append("-");
            if (locRange.getRight() != null)
                dataToHash.append(locRange.getRight());
        }
        if (excludeLocRange != null) {
            dataToHash.append(",excludeLocRange=");
            if (excludeLocRange.getLeft() != null)
                dataToHash.append(excludeLocRange.getLeft()).append("-");
            if (excludeLocRange.getRight() != null)
                dataToHash.append(excludeLocRange.getRight());
        }
        if (reviewerCount != null) {
            dataToHash.append(",reviewerCount=");
            if (reviewerCount.getLeft() != null)
                dataToHash.append(reviewerCount.getLeft()).append("-");
            if (reviewerCount.getRight() != null)
                dataToHash.append(reviewerCount.getRight());
        }
        if (approverCount != null) {
            dataToHash.append(",approverCount=");
            if (approverCount.getLeft() != null)
                dataToHash.append(approverCount.getLeft()).append("-");
            if (approverCount.getRight() != null)
                dataToHash.append(approverCount.getRight());
        }
        if (MapUtils.isNotEmpty(partialMatch)) {
            TreeSet<String> fields = new TreeSet<>(partialMatch.keySet());
            dataToHash.append(",partialMatch=(");
            for (String field : fields) {
                Map<String, String> innerMap = partialMatch.get(field);
                TreeSet<String> innerFields = new TreeSet<>(innerMap.keySet());
                dataToHash.append("(");
                for (String innerField : innerFields) {
                    dataToHash.append(innerField).append("=").append(innerMap.get(innerField)).append(",");
                }
                dataToHash.append("),");
            }
            dataToHash.append(")");
        }
        if (MapUtils.isNotEmpty(codeChangeSizeConfig)) {
            TreeSet<String> fields = new TreeSet<>(codeChangeSizeConfig.keySet());
            dataToHash.append(",codeChangeSizeConfig=(");
            for (String field : fields) {
                dataToHash.append(field.toLowerCase(Locale.ROOT)).append("=")
                        .append(codeChangeSizeConfig.get(field).toLowerCase(Locale.ROOT));
            }
            dataToHash.append(")");
        }
        if (MapUtils.isNotEmpty(commentDensitySizeConfig)) {
            TreeSet<String> fields = new TreeSet<>(commentDensitySizeConfig.keySet());
            dataToHash.append(",commentDensitySizeConfig=(");
            for (String field : fields) {
                dataToHash.append(field.toLowerCase(Locale.ROOT)).append("=")
                        .append(commentDensitySizeConfig.get(field).toLowerCase(Locale.ROOT));
            }
            dataToHash.append(")");
        }
        if (MapUtils.isNotEmpty(sort)) {
            TreeSet<String> fields = new TreeSet<>(sort.keySet());
            dataToHash.append(",sort=(");
            for (String field : fields) {
                dataToHash.append(field.toLowerCase(Locale.ROOT)).append("=")
                        .append(sort.get(field).toString().toLowerCase(Locale.ROOT));
            }
            dataToHash.append(")");
        }
        return dataToHash.toString();
    }

    public String generateCacheHash() {
        return CacheHashUtils.generateCacheHash(generateCacheRawString());
    }

    @SuppressWarnings("unchecked")
    public static ScmPrFilter fromDefaultListRequest(DefaultListRequest filter, DISTINCT across, CALCULATION calculation) throws BadRequestException {
        Map<String, Map<String, String>> partialMatchMap = MapUtils.emptyIfNull((Map<String, Map<String, String>>) filter.getFilter().get("partial_match"));
        Map<String, Object> excludedFields = (Map<String, Object>) filter.getFilter().getOrDefault("exclude", Map.of());
        Map<String, Map<String, String>> excludePartialMatchMap = MapUtils.emptyIfNull((Map<String, Map<String, String>>) excludedFields.get("partial_match"));
        Map<String, String> codeChangeConfigMap = MapUtils.emptyIfNull((Map<String, String>) filter.getFilter().get("code_change_size_config"));
        Map<String, String> commentDensityConfigMap = MapUtils.emptyIfNull((Map<String, String>) filter.getFilter().get("comment_density_size_config"));
        Map<String, SortingOrder> sort = SortingConverter.fromFilter(MoreObjects.firstNonNull(filter.getSort(), List.of()));
        List<String> orgProductIdsList = getListOrDefault(filter, "org_product_ids");
        Set<UUID> orgProductIdsSet = orgProductIdsList.stream().map(UUID::fromString).collect(Collectors.toSet());
        ScmPrFilterBuilder bldr = ScmPrFilter.builder()
                .prCreatedRange(getTimeRange(filter, "pr_created_at"))
                .prMergedRange(getTimeRange(filter, "pr_merged_at"))
                .prClosedRange(getTimeRange(filter, "pr_closed_at"))
                .prUpdatedRange(getTimeRange(filter, "pr_updated_at"))
                .approverCount(getNumericRange(filter, "num_approvers"))
                .reviewerCount(getNumericRange(filter, "num_reviewers"))
                .locRange(getNumericRange(filter, "loc"))
                .labels(getListOrDefault(filter, "labels"))
                .states(getListOrDefault(filter, "states"))
                .repoIds(getListOrDefault(filter, "repo_ids"))
                .projects(getListOrDefault(filter, "projects"))
                .commitTitles(getListOrDefault(filter, "commit_titles"))
                .sourceBranches(CollectionUtils.isNotEmpty(getListOrDefault(filter, "source_branches")) ?
                        getListOrDefault(filter, "source_branches") : getListOrDefault(filter, "branches"))
                .targetBranches(getListOrDefault(filter, "target_branches"))
                .hasIssueKeys(StringUtils.defaultIfEmpty((String) filter.getFilter().get("has_issue_keys"), StringUtils.EMPTY))
                .missingFields(MapUtils.emptyIfNull(
                        (Map<String, Boolean>) filter.getFilter().get("missing_fields")))
                .approvers(getListOrDefault(filter, "approvers"))
                .commenters(getListOrDefault(filter, "commenters"))
                .reviewTypes(getListOrDefault(filter, "review_types"))
                .creators(getListOrDefault(filter, "creators"))
                .reviewers(getListOrDefault(filter, "reviewers"))
                .titles(getListOrDefault(filter, "titles"))
                .assignees(getListOrDefault(filter, "assignees"))
                .codeChanges(getListOrDefault(filter, "code_change_sizes"))
                .commentDensities(getListOrDefault(filter, "comment_densities"))
                .approvalStatuses(getListOrDefault(filter, "approval_statuses"))
                .collabStates(getListOrDefault(filter, "collab_states"))
                .codeChangeSizeConfig(codeChangeConfigMap)
                .commentDensitySizeConfig(commentDensityConfigMap)
                .codeChangeUnit(StringUtils.defaultIfEmpty((String) filter.getFilter().get("code_change_size_unit"), "lines"))
                .aggInterval(MoreObjects.firstNonNull(
                        AGG_INTERVAL.fromString(filter.getAggInterval()), AGG_INTERVAL.day))
                .orgProductIds(orgProductIdsSet)
                .integrationIds(getListOrDefault(filter, "integration_ids"))
                .isApplyOuOnVelocityReport(filter.getApplyOuOnVelocityReport())
                .partialMatch(partialMatchMap)
                .excludePartialMatch(excludePartialMatchMap)
                .hasComments(filter.getFilter().get("has_comments") != null ? (Boolean) filter.getFilter().get("has_comments") : false)
                .excludeRepoIds(getListOrDefault(excludedFields, "repo_ids"))
                .excludeProjects(getListOrDefault(excludedFields, "projects"))
                .excludeCommitTitles(getListOrDefault(excludedFields, "commit_titles"))
                .excludeCreators(getListOrDefault(excludedFields, "creators"))
                .excludeSourceBranches(CollectionUtils.isNotEmpty(getListOrDefault(excludedFields, "source_branches")) ?
                        getListOrDefault(excludedFields, "source_branches") : getListOrDefault(excludedFields, "branches"))
                .excludeTargetBranches(getListOrDefault(excludedFields, "target_branches"))
                .excludeReviewers(getListOrDefault(excludedFields, "reviewers"))
                .excludeTitles(getListOrDefault(excludedFields, "titles"))
                .excludeStates(getListOrDefault(excludedFields, "states"))
                .excludeLabels(getListOrDefault(excludedFields, "labels"))
                .excludeReviewTypes(getListOrDefault(excludedFields, "review_types"))
                .excludeAssignees(getListOrDefault(excludedFields, "assignees"))
                .excludeApprovalStatuses(getListOrDefault(excludedFields, "approval_statuses"))
                .excludeCommentDensities(getListOrDefault(excludedFields, "comment_densities"))
                .excludeCodeChanges(getListOrDefault(excludedFields, "code_change_sizes"))
                .excludeLocRange(getExcludedRange(filter, "", "loc"))
                .excludeApprovers(getListOrDefault(excludedFields, "approvers"))
                .excludeCommenters(getListOrDefault(excludedFields, "commenters"))
                .excludeCollabStates(getListOrDefault(excludedFields, "collab_states"))
                .sort(sort);

        if (across != null) {
            bldr.across(across);
        }
        if (calculation != null) {
            bldr.calculation(calculation);
        }
        ScmPrFilter prsFilter = bldr.build();
        log.info("prsFilter = {}", prsFilter);
        return prsFilter;
    }
}
