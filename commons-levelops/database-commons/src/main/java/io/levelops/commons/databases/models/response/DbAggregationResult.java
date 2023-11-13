package io.levelops.commons.databases.models.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.jackson.DefaultObjectMapper;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

@Value
@Log4j2
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = DbAggregationResult.DbAggregationResultBuilder.class)
public class DbAggregationResult {

    public static final List<String> PREDICTIONS_FIELDS = List.of("total_tickets", "count");
    public static final Map<String, Function<DbAggregationResult, Double>> GET_MAP = new HashMap<>() {{
        put("count",
                agg -> agg.getCount() == null ? null : agg.getCount().doubleValue());
        put("total_tickets",
                agg -> agg.getTotalTickets() == null ? null : agg.getTotalTickets().doubleValue());
        put("total_cases",
                agg -> agg.getTotalCases() == null ? null : agg.getTotalCases().doubleValue());
        put("total_tests",
                agg -> agg.getTotalTests() == null ? null : agg.getTotalTests().doubleValue());
        put("total_issues",
                agg -> agg.getTotalIssues() == null ? null : agg.getTotalIssues().doubleValue());
        put("total_defects",
                agg -> agg.getTotalDefects() == null ? null : agg.getTotalDefects().doubleValue());
        put("duplicated_density",
                agg -> agg.getDuplicatedDensity() == null ? null : agg.getDuplicatedDensity().doubleValue());
        put("total_story_points",
                agg -> agg.getTotalStoryPoints() == null ? null : agg.getTotalStoryPoints().doubleValue());
        put("total_unestimated_tickets",
                agg -> agg.getTotalUnestimatedTickets() == null ? null : agg.getTotalUnestimatedTickets().doubleValue());
        put("lines_added_count",
                agg -> agg.getLinesAddedCount() == null ? null : agg.getLinesAddedCount().doubleValue());
        put("lines_removed_count",
                agg -> agg.getLinesRemovedCount() == null ? null : agg.getLinesRemovedCount().doubleValue());
        put("files_changed_count",
                agg -> agg.getFilesChangedCount() == null ? null : agg.getFilesChangedCount().doubleValue());
    }};


    public static final Map<String, BiFunction<Double,
            DbAggregationResult.DbAggregationResultBuilder,
            DbAggregationResult>> SET_MAP = new HashMap<>() {{
        put("count",
                (val, builder) -> builder.count(Math.round(val)).build());
        put("total_tickets",
                (val, builder) -> builder.totalTickets(Math.round(val)).build());
        put("total_cases",
                (val, builder) -> builder.totalCases(Math.round(val)).build());
        put("total_tests",
                (val, builder) -> builder.totalTests(Math.round(val)).build());
        put("total_issues",
                (val, builder) -> builder.totalIssues(Math.round(val)).build());
        put("total_defects",
                (val, builder) -> builder.totalDefects(Math.round(val)).build());
        put("duplicated_density",
                (val, builder) -> builder.duplicatedDensity(val.floatValue()).build());
        put("total_story_points",
                (val, builder) -> builder.totalStoryPoints(Math.round(val)).build());
        put("total_unestimated_tickets",
                (val, builder) -> builder.totalUnestimatedTickets(Math.round(val)).build());
        put("lines_added_count",
                (val, builder) -> builder.linesAddedCount(Math.round(val)).build());
        put("lines_removed_count",
                (val, builder) -> builder.linesRemovedCount(Math.round(val)).build());
        put("files_changed_count",
                (val, builder) -> builder.filesChangedCount(Math.round(val)).build());
    }};


    @JsonProperty("key")
    String key;
    @JsonProperty("additional_key")
    String additionalKey;

    @JsonProperty("cicd_job_id")
    String ciCdJobId;
    @JsonProperty("repo_id")
    String repoId;
    @JsonProperty("repo_ids")
    List<String> repoIds;
    @JsonProperty("project")
    String project;
    @JsonProperty("organization")
    String organization;
    @JsonProperty("branch")
    String branch;
    @JsonProperty("pr_number")
    Integer prNumber;
    @JsonProperty("pr_link")
    String prLink;

    @JsonProperty("title")
    String title;
    @JsonProperty("day_of_week")
    String dayOfWeek;

    @JsonProperty("median")
    Long median;
    @JsonProperty("min")
    Long min;
    @JsonProperty("max")
    Long max;
    @JsonProperty("count")
    Long count;
    @JsonProperty("sum")
    Long sum;
    @JsonProperty("mean")
    Double mean;
    @JsonProperty("mean_story_points")
    Double meanStoryPoints;
    @JsonProperty("p90")
    Long p90;
    @JsonProperty("p95")
    Long p95;
    @JsonProperty("commit_size")
    Long commitSize;
    @JsonProperty("bugs")
    Long bugs;
    @JsonProperty("vulnerabilities")
    Long vulnerabilities;
    @JsonProperty("code_smells")
    Long codeSmells;

    //Bollinger Results
    @JsonProperty("bol_avg")
    Double bollingerAvg;
    @JsonProperty("bol_std")
    Double bollingerStd;

    //Prediction Results
    @JsonProperty("prediction_lower_bound")
    Double predictionLowerBound;
    @JsonProperty("prediction")
    Double prediction;
    @JsonProperty("prediction_upper_bound")
    Double predictionUpperBound;

    // generic total for new aggs going forward
    @JsonProperty("total")
    Long total;
    @JsonProperty("total_tickets")
    Long totalTickets;
    @JsonProperty("total_cases")
    Long totalCases;
    @JsonProperty("total_tests")
    Long totalTests;
    @JsonProperty("total_issues")
    Long totalIssues;
    @JsonProperty("total_defects")
    Long totalDefects;
    @JsonProperty("duplicated_density")
    Float duplicatedDensity;
    @JsonProperty("total_story_points")
    Long totalStoryPoints;
    @JsonProperty("total_effort")
    Long totalEffort;
    @JsonProperty("total_unestimated_tickets")
    Long totalUnestimatedTickets;

    @JsonProperty("total_lines_added")
    Long linesAddedCount;

    @JsonProperty("total_lines_removed")
    Long linesRemovedCount;

    @JsonProperty("total_comments")
    Long totalComments;

    @JsonProperty("total_lines_changed")
    Long linesChangedCount;

    @JsonProperty("total_files_changed")
    Long filesChangedCount;

    @JsonProperty("avg_change_size")
    Float avgChangeSize;
    @JsonProperty("median_change_size")
    Long medianChangeSize;
    @JsonProperty("pct_new_lines")
    Double pctNewLines;
    @JsonProperty("pct_refactored_lines")
    Double pctRefactoredLines;
    @JsonProperty("pct_legacy_refactored_lines")
    Double pctLegacyRefactoredLines;

    @JsonProperty("avg_files_changed")
    Double avgFilesChanged;

    @JsonProperty("avg_lines_changed")
    Double avgLinesChanged;

    @JsonProperty("median_lines_changed")
    Double medianLinesChanged;

    @JsonProperty("median_files_changed")
    Double medianFilesChanged;

    @JsonProperty("deploy_job_runs_count")
    Long deployJobRunsCount;

    //region Collaboration Report
    @JsonProperty("collab_state")
    String collabState;
    //endregion

    //region Dora Metric
    @JsonProperty("band")
    String band;
    @JsonProperty("deployment_frequency")
    Double deploymentFrequency;
    @JsonProperty("failure_rate")
    Double failureRate;
    @JsonProperty("lead_time")
    Long leadTime;
    @JsonProperty("recover_time")
    Long recoverTime;

    //endregion
    @JsonProperty("stacks")
    List<DbAggregationResult> stacks;

    @JsonProperty("data")
    List<DbAggregationResult> data;

    // code coverage metrics
    @JsonProperty("additional_counts")
    Map<String, Object> additionalCounts;

    @JsonProperty("assignees")
    List<String> assignees;

    // region sprint metrics
    @JsonProperty("integration_id")
    String integrationId;
    @JsonProperty("sprint_id")
    String sprintId;
    @JsonProperty("sprint_name")
    String sprintName;
    @JsonProperty("sprint_goal")
    String sprintGoal;
    @JsonProperty("sprint_completed_at")
    Long sprintCompletedAt;
    @JsonProperty("sprint_started_at")
    Long sprintStartedAt;
    @JsonProperty("sprint_mapping_aggs")
    List<JiraIssueSprintMappingAggResult> sprintMappingAggs;
    @JsonProperty("issue_mgmt_sprint_mapping_aggs")
    List<IssueMgmtSprintMappingAggResult> issueMgmtSprintMappingAggResults;
    // end region

    // region priority
    String priority;
    Integer priorityOrder;
    // endregion

    //region Velocity
    @JsonProperty("velocity_stage_result")
    VelocityStageResult velocityStageResult;
    // endregion

    //region BA
    @JsonProperty("fte")
    Float fte;
    @JsonProperty("effort")
    Long effort;
    @JsonProperty("alignment_score")
    Integer alignmentScore; // score based on category goals
    @JsonProperty("percentage_score")
    Float percentageScore;
    @JsonProperty("category_allocations")
    Map<String, BaAllocation> categoryAllocations;
    //endregion

    //region trellis
    @JsonProperty("time_spent_per_ticket")
    Long timeSpentPerTicket;
    //end region


    //region Stage Bounce Report
    @JsonProperty("stage")
    String stage;
    //endregion

    public String toString() {
        return DefaultObjectMapper.writeAsPrettyJson(this);
    }
}
