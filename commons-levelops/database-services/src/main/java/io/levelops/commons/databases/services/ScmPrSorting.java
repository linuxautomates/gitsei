package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.filters.ScmPrFilter;

import java.util.Set;

public class ScmPrSorting {
    public static final String MEDIAN_AUTHOR_RESPONSE_TIME = "median_author_response_time";
    public static final String MEAN_AUTHOR_RESPONSE_TIME = "mean_author_response_time";
    public static final String MEDIAN_REVIEWER_RESPONSE_TIME = "median_reviewer_response_time";
    public static final String MEAN_REVIEWER_RESPONSE_TIME = "mean_reviewer_response_time";
    public static final Set<String> PR_SORTABLE_METRICS = Set.of(MEDIAN_AUTHOR_RESPONSE_TIME, MEAN_AUTHOR_RESPONSE_TIME, MEDIAN_REVIEWER_RESPONSE_TIME, MEAN_REVIEWER_RESPONSE_TIME);
    public static final Set<String> PR_SORTABLE_LABEL = Set.of(ScmPrFilter.DISTINCT.repo_id.toString(),
            ScmPrFilter.DISTINCT.creator.toString(),
            ScmPrFilter.DISTINCT.project.toString(),
            ScmPrFilter.DISTINCT.branch.toString(),
            ScmPrFilter.DISTINCT.source_branch.toString(),
            ScmPrFilter.DISTINCT.reviewer.toString());
    public static final Set<String> PR_SORTABLE = Set.of(MEDIAN_AUTHOR_RESPONSE_TIME, MEAN_AUTHOR_RESPONSE_TIME, MEDIAN_REVIEWER_RESPONSE_TIME, MEAN_REVIEWER_RESPONSE_TIME,
            ScmPrFilter.DISTINCT.repo_id.toString(),
            ScmPrFilter.DISTINCT.creator.toString(),
            ScmPrFilter.DISTINCT.project.toString(),
            ScmPrFilter.DISTINCT.branch.toString(),
            ScmPrFilter.DISTINCT.source_branch.toString(),
            ScmPrFilter.DISTINCT.reviewer.toString());
    public static final Set<String> PR_SORTABLE_COLUMNS = Set.of("pr_updated_at", "pr_merged_at", "pr_created_at", "pr_closed_at",
            "title", "project", "assignees", "approver", "reviewer", "creator", "cycle_time", "lines_added", "lines_deleted"
            , "lines_changed", "files_ct", "repo_id", "approval_status", "review_type");
}
