import { optionType } from "dashboard/dashboard-types/common-types";
import { JiraReports, JiraStatReports } from "../../constants/enums/jira-reports.enum";
import { SCMVisualizationTypes, SCMReworkVisualizationTypes } from "../../constants/typeConstants";

export const ITEM_TEST_ID = "filter-list-element";

export const LEGACY_CODE_INFO =
  "Files that haven't been modified in a long time are considered as legacy code. Define the time range for legacy code.";

export enum depencyAnalysisOptionBEKeys {
  IS_BLOCKED_BY = "is blocked by",
  BLOCKS = "blocks",
  RELATED_TO = "relates to"
}

export const dependencyAnalysisFilterOptions: optionType[] = [
  { label: "Select issues blocked by filtered issues", value: "blocks" },
  { label: "Select issues blocking filtered issues", value: "is blocked by" },
  { label: "Select issues related to filtered issues", value: "relates to" }
];

export const CreatedAtUpdateAtOptions = [
  {
    id: "last_week",
    label: "Last 7 days",
    mFactor: 7
  },
  {
    id: "last_month",
    label: "Last 30 days",
    mFactor: 30
  },
  {
    id: "last_quarter",
    label: "Last 90 days",
    mFactor: 90
  },
  {
    id: "last_6_months",
    label: "Last 180 days",
    mFactor: 180
  },
  {
    id: "last_year",
    label: "Last 365 days",
    mFactor: 365
  }
];

export const ExtendedCreatedAtUpdateAtOptions = [
  {
    id: "last_week",
    label: "Last 7 days",
    mFactor: 7
  },
  {
    id: "last_2_week",
    label: "Last 2 weeks",
    mFactor: 14
  },
  {
    id: "last_month",
    label: "Last 30 days",
    mFactor: 30
  },
  {
    id: "last_quarter",
    label: "Last 90 days",
    mFactor: 90
  },
  {
    id: "last_6_months",
    label: "Last 180 days",
    mFactor: 180
  },
  {
    id: "last_year",
    label: "Last 365 days",
    mFactor: 365
  }
];

export const SnapshotTimeRangeOption = [
  {
    id: "last_4_week",
    label: "Last 4 weeks",
    mFactor: 28
  },
  {
    id: "last_4_month",
    label: "Last 4 months",
    mFactor: 120
  },
  {
    id: "last_4_quarter",
    label: "Last 4 quarters",
    mFactor: 360
  }
];

export const jiraEffortInvestmentTrendReportTimeRangeOptions = [
  {
    id: "last_4_week",
    label: "Last 4 weeks",
    mFactor: 7
  },
  {
    id: "last_4_month",
    label: "Last 4 months",
    mFactor: 30
  },
  {
    id: "last_4_quarter",
    label: "Last 4 quarters",
    mFactor: 120
  }
];

export const jiraEpicPriorityTimeRangeFilterOption = [
  {
    id: "last_4_week",
    label: "Last 4 weeks",
    mFactor: 7
  },
  {
    id: "last_4_month",
    label: "Last 4 months",
    mFactor: 30
  }
];

// These filters are used as time period options
export const jiraEffortInvestmentTimeRangeFilterOption = [
  {
    label: "Last 7 days",
    value: 7
  },
  {
    label: "Last 2 Weeks",
    value: 14
  },
  {
    label: "Last Month",
    value: 30
  },
  {
    label: "Last 3 Months",
    value: 90
  }
];

export const effortInvestmentTrendReportSampleInterval = [
  { label: "Week", value: "week" },
  { label: "Two Weeks", value: "biweekly" },
  { label: "Month", value: "month" },
  { label: "Quarter", value: "quarter" }
];

export const effortInvestmentTeamTimeRangeFilterOption = [
  {
    id: "last_4_week",
    label: "Last 4 weeks",
    mFactor: 7
  },
  {
    id: "last_8_week",
    label: "Last 8 weeks",
    mFactor: 14
  },
  {
    id: "last_4_month",
    label: "Last 4 months",
    mFactor: 30
  }
];

export const ZendeskCreatedInFilterOptions = [
  {
    id: "last_week",
    label: "Last Week",
    mFactor: 7
  },
  {
    id: "last_month",
    label: "Last Month",
    mFactor: 30
  },
  {
    id: "last_quarter",
    label: "Last Quarter",
    mFactor: 90
  }
];

export const completedDateOptions = [
  {
    id: "last_week",
    label: "Last week",
    mFactor: 7
  },
  {
    id: "last_2_weeks",
    label: "Last 2 Weeks",
    mFactor: 14
  },
  {
    id: "last_month",
    label: "Last Month",
    mFactor: 30
  },
  {
    id: "last_3_months",
    label: "Last 3 Months",
    mFactor: 90
  }
];

export const sprintEndAtOptions = [
  {
    id: "last_week",
    label: "Last week",
    mFactor: 7
  },
  {
    id: "last_2_weeks",
    label: "Last 2 Weeks",
    mFactor: 14
  },
  {
    id: "last_month",
    label: "Last Month",
    mFactor: 30
  },
  {
    id: "last_3_months",
    label: "Last 3 Months",
    mFactor: 90
  },
  {
    id: "last_6_months",
    label: "Last 6 Months",
    mFactor: 180
  },
  {
    id: "last_year",
    label: "Last Year",
    mFactor: 365
  }
];

export const leadTimeOptions = [
  {
    id: "last_2_weeks",
    label: "Last 2 Weeks",
    mFactor: 14
  },
  {
    id: "last_month",
    label: "Last Month",
    mFactor: 30
  },
  {
    id: "last_3_months",
    label: "Last 3 Months",
    mFactor: 90
  },
  {
    id: "last_year",
    label: "Last year",
    mFactor: 365
  }
];

export const sonarQubeMetricsOptions = [
  { value: "complexity", label: "Cyclomatic Complexity" },
  { value: "cognitive_complexity", label: "Cognitive Complexity" }
];

export const codeVolVsDeploymentMetricsOptions = [
  { value: "line_count", label: "Line Count" },
  { value: "file_count", label: "File Count" }
];

export const jiraResolutionTimeMetricsOptions = [
  { value: "median_resolution_time", label: "Median Resolution Time" },
  { value: "number_of_tickets_closed", label: "Number Of Tickets" },
  { value: "90th_percentile_resolution_time", label: "90th Percentile Resolution Time" },
  { value: "average_resolution_time", label: "Average Resolution Time" }
];

export const scmResolutionTimeMetricsOptions = [
  { value: "median_resolution_time", label: "Median Resolution Time" },
  { value: "number_of_tickets_closed", label: "Number Of Tickets" }
];

export const jiraSprintCommitToDoneMetricsOptions = [{ value: "avg_commit_to_done", label: "Ave. Done to Commit" }];

export const jiraSprintCreepMetricsOptions = [{ value: "avg_creep", label: "Ave. Sprint Creep" }];

export const jiraSprintCreepToDoneMetricsOptions = [{ value: "avg_creep_to_done", label: "Ave. Sprint Creep Done" }];

export const jiraSprintPercentageTrendMetricsOptions = [
  { value: "done_to_commit_ratio", label: "Delivered to Commit Ratio" },
  { value: "creep_to_commit_ratio", label: "Creep to Commit Ratio" },
  { value: "creep_done_to_commit_ratio", label: "Creep Done to Commit Ratio" },
  { value: "creep_done_ratio", label: "Creep Done Ratio" },
  { value: "creep_missed_ratio", label: "Creep Missed Ratio" },
  { value: "commit_missed_ratio", label: "Commit Missed Ratio" },
  { value: "commit_done_ratio", label: "Commit Done Ratio" }
];

export const jiraSprintTrendMetricsOptions = [
  { value: "commit_not_done_points", label: "Commit missed" },
  { value: "commit_done_points", label: "Commit done" },
  { value: "creep_done_points", label: "Creep done" },
  { value: "creep_not_done_points", label: "Creep missed" }
];

export const ticketReportMetricOptions = [
  { value: "ticket", label: "Number of tickets" },
  { value: "story_point", label: "Sum of story points" }
];

export const jiraResolutionTimeGroupByOptions = [
  { value: "week", label: "Last Closed Week" },
  { value: "month", label: "Last Closed Month" },
  { value: "quarter", label: "Last Closed Quarter" }
];

const CIRCLE_CHART = 1;
const LINE_CHART = 4;
const BAR_CHART = 3;

export const graphTypeOptions = [
  { value: BAR_CHART, label: "Bar" },
  { value: LINE_CHART, label: "Line" },
  { value: CIRCLE_CHART, label: "Pie Chart" }
];

export const defaultTimePeriodOptions = [
  {
    label: "Last day",
    value: 1
  },
  {
    label: "Last 7 days",
    value: 7
  },
  {
    label: "Last 2 Weeks",
    value: 14
  },
  {
    label: "Last 30 days",
    value: 30
  }
];

export const sonarQubeCodeComplexityWigets = [
  "sonarqube_code_complexity_report",
  "sonarqube_code_complexity_trend_report"
];

export const backlogTrendReportOptions = [
  { value: "week", label: "Weekly on Monday" },
  { value: "month", label: "First day of the month" },
  { value: "quarter", label: "First day of the quarter" }
];

export const azureIntervalReport = [
  { value: "day", label: "Day" },
  { value: "week", label: "Week" },
  { value: "month", label: "Month" },
  { value: "quarter", label: "Quarter" },
  { value: "year", label: "Year" }
];

export const hygieneIntervalReport = [
  { value: "day", label: "Daily" },
  { value: "week", label: "Weekly" },
  { value: "month", label: "Monthly" },
  { value: "quarter", label: "Quarterly" }
];

export const cicdIntervalOptions = [
  { value: "day", label: "Daily" },
  { value: "week", label: "Weekly" },
  { value: "month", label: "Monthly" },
  { value: "quarter", label: "Quarterly" },
  { value: "year", label: "Yearly" }
];

export const sprintImpactIntervalOptions = [
  { value: "week", label: "Weekly on Monday" },
  { value: "bi-weekly", label: "Every two weeks on Monday" },
  { value: "month", label: "Every month on the first Monday" }
];

export const backlogMatricsOptions = [
  { value: "median", label: "Median Age" },
  { value: "p90", label: "90th percentile age" },
  { value: "mean", label: "Average age" },
  { value: "total_tickets", label: "Number of tickets" }
];

export const leadTimeMetricOptions = [
  { value: "mean", label: "Average time in stage" },
  { value: "median", label: "Median time in stage" },
  { value: "p90", label: "90th percentile time in stage" },
  { value: "p95", label: "95th percentile time in stage" }
];

export const demoLeadTimeMetricOptions = [
  { value: "mean", label: "Average time in stage", disabled: false },
  { value: "median", label: "Median time in stage", disabled: true },
  { value: "p90", label: "90th percentile time in stage", disabled: true },
  { value: "p95", label: "95th percentile time in stage", disabled: true }
];

export const newLeadTimeMetricOptions = [
  {
    value: "mean",
    label: "Average time",
    headerText: "Total time",
    tooltip: "Combined average mean of the lead time across stages"
  },
  {
    value: "median",
    label: "Median time",
    headerText: "Total time",
    tooltip: "Combined median of the lead time across stages"
  },
  {
    value: "p90",
    label: "90th percentile time",
    headerText: "Total time",
    tooltip: "Combined 90th percentile of the lead time across stages"
  },
  {
    value: "p95",
    label: "95th percentile time",
    headerText: "Total time",
    tooltip: "Combined 95th percentile of the lead time across stages"
  }
];

export const jiraBacklogKeyMapping = {
  median: "Median Age of Tickets",
  p90: "90th Percentile Age of Tickets",
  mean: "Average Age of Tickets",
  total_tickets: "Number of tickets",
  total_story_points: "Sum of Story Points"
};

export const scmResolutionTimeTooltipMapping = {
  number_of_tickets_closed: "Number of Tickets"
};

export const backlogVisualizationOptions = [
  { value: "bar_chart", label: "Bar Chart" },
  { value: "line_chart", label: "Line Chart" }
];

export const sprintVisualizationOptions = [
  { value: "stacked_area", label: "Stacked Area" },
  { value: "unstacked_area", label: "Unstacked Area" },
  { value: "line", label: "Line" }
];

export const hygieneVisualizationOptions = [
  { value: "stacked_area", label: "Stacked Area Chart" },
  { value: "stacked_bar", label: "Stacked Bar Chart" }
];

export const jiraTimeAcrossStagesMetricOptions = [
  { value: "median_time", label: "Median Time In Status" },
  { value: "average_time", label: "Average Time In Status" }
];

export const jiraTicketsReportTimeAcrossOptions = [
  { label: "Issue Created By Week", value: "issue_created_week" },
  { label: "Issue Created By Month", value: "issue_created_month" },
  { label: "Issue Created By Quarter", value: "issue_created_quarter" },
  { label: "Issue Updated By Week", value: "issue_updated_week" },
  { label: "Issue Updated By Month", value: "issue_updated_month" },
  { label: "Issue Updated By Quarter", value: "issue_updated_quarter" },
  { label: "Issue Resolved By Week", value: "issue_resolved_week" },
  { label: "Issue Resolved By Month", value: "issue_resolved_month" },
  { label: "Issue Resolved By Quarter", value: "issue_resolved_quarter" },
  { label: "Issue Due Date - By Week", value: "issue_due_week" },
  { label: "Issue Due Date - By Month", value: "issue_due_month" },
  { label: "Issue Due Date - By Quarter", value: "issue_due_quarter" }
];

export const azureTicketsReportTimeAcrossOptions = [
  { label: "Workitem Created By Week", value: "workitem_created_at_week" },
  { label: "Workitem Created By Month", value: "workitem_created_at_month" },
  { label: "Workitem Created By Quarter", value: "workitem_created_at_quarter" },
  { label: "Workitem Updated By Week", value: "workitem_updated_at_week" },
  { label: "Workitem Updated By Month", value: "workitem_updated_at_month" },
  { label: "Workitem Updated By Quarter", value: "workitem_updated_at_quarter" },
  { label: "Workitem Resolved By Week", value: "workitem_resolved_at_week" },
  { label: "Workitem Resolved By Month", value: "workitem_resolved_at_month" },
  { label: "Workitem Resolved By Quarter", value: "workitem_resolved_at_quarter" }
];

export const defaultMaxEntriesOptions = [
  { label: "10", value: 10 },
  { label: "20", value: 20 },
  { label: "50", value: 50 },
  { label: "100", value: 100 }
];

export interface StackOptionItem {
  key: string;
  name: string;
  display_name?: string;
}

export const jiraResolutionTimeReports = [
  JiraReports.RESOLUTION_TIME_REPORT,
  JiraReports.RESOLUTION_TIME_REPORT_TRENDS,
  JiraStatReports.RESOLUTION_TIME_COUNTS_STAT
];

export const MODIFIED_API_FILTERS_REPORT = ["code_volume_vs_deployment_report"];

export const scmCodingMetricsOptions = [
  { value: "avg_coding_day_week", label: "Average Coding days per week" },
  { value: "median_coding_day_week", label: "Median Coding days per week" },
  { value: "avg_coding_day_biweekly", label: "Average Coding days per two weeks" },
  { value: "median_coding_day_biweekly", label: "Median Coding days per two weeks" },
  { value: "avg_coding_day_month", label: "Average Coding days per month" },
  { value: "median_coding_day_month", label: "Median Coding days per month" }
];

export const scmCodingSingleStatMetricsOptions = [
  { value: "average_coding_day", label: "Average Coding days" },
  { value: "median_coding_day", label: "Median Coding days" }
];

export const scmCodeChangeOptions = [
  { label: "Small", value: "small" },
  { label: "Medium", value: "medium" },
  { label: "Large", value: "large" }
];

export const scmCommentDensityOptions = [
  { label: "Shallow", value: "shallow" },
  { label: "Good", value: "good" },
  { label: "Heavy", value: "heavy" }
];

export const scmPRsMetricsOptions = [
  { label: "Number of PRs", value: "num_of_prs" },
  { label: "Percentage of filtered PRs", value: "filtered_prs_percentage" }
];

export const scmOtherCriteriaOptions = [
  { label: "Self approved PRs", value: "self approved" },
  { label: "PRs closed without merging", value: "closed without merge" }
];

export const scmCodeChangeSizeUnits = [
  { label: "Lines Of Code", value: "lines" },
  { label: "Files", value: "files" }
];

export const scmVisualizationOptions = [
  { label: "Pie chart", value: SCMVisualizationTypes.CIRCLE_CHART },
  { label: "Bar chart", value: SCMVisualizationTypes.BAR_CHART },
  { label: "Line chart", value: SCMVisualizationTypes.LINE_CHART },
  { label: "Smooth area chart", value: SCMVisualizationTypes.AREA_CHART },
  { label: "Stacked smooth area chart", value: SCMVisualizationTypes.STACKED_AREA_CHART }
];

export const scmPRsResponseTimeMetricsOptions = [
  { value: "average_author_response_time", label: "Average Author Response Time" },
  { value: "median_author_response_time", label: "Median Author Response Time" },
  { value: "average_reviewer_response_time", label: "Average Reviewer Response Time" },
  { value: "median_reviewer_response_time", label: "Median Reviewer Response Time" }
];

export const jiraTicketsSingleStatAcrossOptions = [
  { label: "Issue Created Last Day", value: "issue_created_day" },
  { label: "Issue Created Last Week", value: "issue_created_week" },
  { label: "Issue Created Last Month", value: "issue_created_month" },
  { label: "Issue Created Last Quarter", value: "issue_created_quarter" },
  { label: "Issue Resolved Last Day", value: "issue_resolved_day" },
  { label: "Issue Resolved Last Week", value: "issue_resolved_week" },
  { label: "Issue Resolved Last Month", value: "issue_resolved_month" },
  { label: "Issue Resolved Last Quarter", value: "issue_resolved_quarter" }
];

export const azureTicketsSingleStatAcrossOptions = [
  { label: "Workitem Created Last Day", value: "workitem_created_at_day" },
  { label: "Workitem Created Last Week", value: "workitem_created_at_week" },
  { label: "Workitem Created Last Month", value: "workitem_created_at_month" },
  { label: "Workitem Created Last Quarter", value: "workitem_created_at_quarter" },
  { label: "Workitem Resolved Last Day", value: "workitem_resolved_at_day" },
  { label: "Workitem Resolved Last Week", value: "workitem_resolved_at_week" },
  { label: "Workitem Resolved Last Month", value: "workitem_resolved_at_month" },
  { label: "Workitem Resolved Last Quarter", value: "workitem_resolved_at_quarter" }
];

export const lastFileUpdateIntervalOptions = [
  { label: "Older than 30 days ago", value: 30 },
  { label: "Older than 2 months", value: 60 },
  { label: "Older than 3 months", value: 90 },
  { label: "Older than 6 months", value: 180 },
  { label: "Older than 9 months", value: 270 },
  { label: "Older than 12 months", value: 360 }
];

export const scmReworkVisualizationOptions = [
  { label: "Stacked Bar chart", value: SCMReworkVisualizationTypes.STACKED_BAR_CHART },
  { label: "Percentage Stacked Bar chart", value: SCMReworkVisualizationTypes.PERCENTAGE_STACKED_BAR_CHART }
];

export const pagerdutyTimeToResolveMetricsOptions = [
  { value: "acknowledge", label: "Time To Acknowledge" },
  { value: "resolve", label: "Time To Resolve" }
];

export const pagerdutyTimeToAcknowledgeAxisOptions = [
  { value: "mean", label: "Average Time To Acknowledge" },
  { value: "median", label: "Median Time To Acknowledge" }
];

export const pagerdutyTimeToResolveAxisOptions = [
  { value: "mean", label: "Average Time To Resolve" },
  { value: "median", label: "Median Time To Resolve" }
];

export const pagerdutyTimeToResolveSampleInterval = [
  { value: "day", label: "Day" },
  { value: "week", label: "Week" },
  { value: "month", label: "Month" },
  { value: "quarter", label: "Quarter" },
  { value: "year", label: "Year" }
];

export const timePeriodTointervalMapping = {
  1: "day",
  7: "week",
  14: "week",
  30: "month",
  90: "quarter"
};

export const VALUE_EXCEED_THE_MAXIMUM_VALUE = "VALUE EXCEED THE MAXIMUM LIMIT";

export const stageBounceReportOptions = [
  { value: "mean", label: "Mean Number of Times in stage" },
  { value: "median", label: "Median Number of Times in stage" },
  { value: "total_tickets", label: "Number of tickets" }
];

export const DEV_PRODUCTIVITY_INTERVAL_OPTIONS = [
  { value: "LAST_WEEK", label: "Last Week" },
  { value: "LAST_TWO_WEEKS", label: "Last 2 Weeks" },
  { value: "LAST_TWO_MONTHS", label: "Last 2 Months" },
  { value: "LAST_THREE_MONTHS", label: "Last 3 Months" },
  { value: "LAST_MONTH", label: "Last Month" },
  { value: "LAST_QUARTER", label: "Last Quarter" },
  { value: "LAST_TWO_QUARTERS", label: "Last Two Quarter" },
  { value: "LAST_YEAR", label: "Last Year" }
];

export const APPLY_OU_ON_VELOCITY_OPTIONS = [
  { value: true, label: "apply filters to all nodes" },
  { value: false, label: "apply filters only for the initial node" }
];

export const REPORT_NOT_ALLOWED_FOR_COMPUTATIONAL_MODEL: Array<string> = [
  "lead_time_by_time_spent_in_stages_report",
  "dora_lead_time_for_change",
  "dora_mean_time_to_restore",
  "jira_release_table_report"
];
export const REPORT_NOT_ALLOWED_FOR_CONFIGURE_PROFILE_IN_WIDGET: Array<string> = [
  "dora_lead_time_for_change",
  "dora_mean_time_to_restore"
];

export const SELETE_JIRA_ONLY_PROFILE_TITLE = "Select the Jira based velocity lead time profile for this report";
export const SELETE_JIRA_ONLY_PROFILE_NOTE =
  "Note : This widget can only be associated with Jira based velocity lead time profile with Release stage enabled.";
export const SELETE_JIRA_ONLY_PROFILE_NOT_FIND =
  "Not finding any relevant velocity workflow profile with Jira as the issue management tool?";
export const CREATE_JIRA_ONLY_PROFILE = "Click here to create a new profile";
export const NOT_JIRA_ONLY_PROFILE_MESSAGE = "This profile is not JIRA only based workflow profile.";
export const NOT_ALLOWED_FILTER_LAST_SPRINT = "You can not use this filter along with Filter by last sprint.";
export const TESTRAIL_INCOMPATIBLE_FILTERS =
  "Note : Incompatible filters for the metric test cases will be removed from the widget automatically.";
export const RESOLUTION_TIME_INCOMPATIBLE_FILTERS =
  "Note : Selecting multiple metrics is not allowed when 'Sort X-Axis' is chosen as 'By Value'.";
export const RESOLUTION_TIME_INCOMPATIBLE_XAXIS =
  "Note : Sorting the X-Axis By Value is not available when multiple metrics are selected.";
export const SCM_PRS_REPORT_STACK_DISABLED_MSG =
  "Note: The stacking option is available exclusively for Bar chart visualizations.";
