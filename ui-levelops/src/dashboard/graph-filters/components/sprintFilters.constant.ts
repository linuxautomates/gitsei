import { AZURE_SPRINT_REPORTS, JIRA_SPRINT_REPORTS } from "dashboard/constants/applications/names";

export const IDEAL_FILTER_MIN = "min";
export const IDEAL_FILTER_MAX = "max";
export const IDEAL_RANGE_FILTER_KEY = "ideal_range"; // TODO : change this key

export const sprintStatReports = [
  JIRA_SPRINT_REPORTS.SPRINT_METRICS_SINGLE_STAT,
  AZURE_SPRINT_REPORTS.SPRINT_METRICS_SINGLE_STAT
];
export const jiraSprintReports = [
  JIRA_SPRINT_REPORTS.SPRINT_METRICS_PERCENTAGE_TREND,
  JIRA_SPRINT_REPORTS.SPRINT_METRICS_TREND
];

export enum sprintReportDataKeyTypes {
  COMMITTED_KEYS = "committed_keys",
  COMMITED_STORY_POINTS = "committed_story_points",
  CREEP_KEYS = "creep_keys",
  CREEP_STORY_POINTS = "creep_story_points",
  DELIVERED_CREEP_KEYS = "delivered_creep_keys",
  DELIVERED_CREEP_STORY_POINTS = "delivered_creep_story_points",
  DELIVERED_KEYS = "delivered_keys",
  DELIVERED_STORY_POINTS = "delivered_story_points",
  COMMIT_DELIVERED_KEYS = "commit_delivered_keys",
  COMMIT_DELIVERED_STORY_POINTS = "commit_delivered_story_points",
  TOTAL_ISSUES = "total_issues",
  TOTAL_WORKITEMS = "total_workitems",
  TOTAL_UNESTIMATED_ISSUES = "total_unestimated_issues",
  TOTAL_UNESTIMATED_WORKITEMS = "total_unestimated_workitems",
  STORY_POINTS_BY_ISSUE = "story_points_by_issue",
  STORY_POINTS_BY_WORKITEM = "story_points_by_workitem",
  AVG_CHURN_RATE = "average_churn_rate",
  SPRINT_CHURN_RATE = "sprint_churn_rate",
  STORY_POINTS_PLANNED = "story_points_planned",
  STORY_POINTS_ADDED = "story_points_added",
  STORY_POINTS_REMOVED = "story_points_removed",
  STORY_POINTS_CHANGED = "story_points_changed"
}

// This enum is for the metrics value which are not directly associated to back-end keys. (e.g. average_churn_rate is same as BE key, that's why it's declared in sprintReportDataKeyTypes)
export enum spritnMetricKeyTypes {
  AVG_TICKET_SIZE_PER_SPRINT = "average_ticket_size_per_sprint"
}

// mapping for STD Metric with it's calculative matric
export const metricSTDMapping: any = {
  avg_commit_to_done_std: "avg_commit_to_done",
  commit_to_done_std: "commit_to_done",
  avg_creep_std: "avg_creep",
  velocity_points_std: "velocity_points"
};

type sprintMetricOptionType = { label: string; value: string };

//  stat sprint report opitons
export const statSprintMetricsOptions: sprintMetricOptionType[] = [
  /**
   * Ratios
   */
  { label: "Delivered to Commit Ratio", value: "avg_commit_to_done" },
  { label: "Creep to Commit Ratio", value: "avg_creep" },
  { label: "Creep Done to Commit Ratio", value: "avg_creep_done_to_commit" },
  { label: "Creep Done Ratio", value: "avg_creep_to_done" },
  { label: "Creep Missed Ratio", value: "avg_creep_to_miss" },
  { label: "Commit Missed Ratio", value: "avg_commit_to_miss" },
  { label: "Commit Done Ratio", value: "commit_to_done" },
  /**
   * Ratios STD
   */
  { label: "Delivered to Commit Ratio STDEV", value: "avg_commit_to_done_std" },
  { label: "Commit Done Ratio STDEV", value: "commit_to_done_std" },
  { label: "Creep to Commit Ratio STDEV", value: "avg_creep_std" },
  /**
   * Points
   */
  { label: "Velocity Points", value: "velocity_points" },
  { label: "Creep Points", value: "creep_points" },
  { label: "Creep Done Points", value: "creep_done_points" },
  { label: "Velocity Points STDEV", value: "velocity_points_std" },
  { label: "Commit Missed Points", value: "commit_to_miss_points" },
  { label: "Creep Missed Points", value: "creep_to_miss_points" },
  { label: "Commit Done Points", value: "commit_to_done_points" },
  { label: "Missed Points", value: "missed_points" },
  { label: "Commit Points", value: "commit_points" },
  /**
   * Tickets
   */
  { label: "Creep Tickets", value: "creep_tickets" },
  { label: "Creep Done Tickets", value: "creep_done_tickets" },
  { label: "Creep Missed Tickets", value: "creep_missed_tickets" },
  { label: "Commit Tickets", value: "commit_tickets" },
  { label: "Commit Done Tickets", value: "commit_done_tickets" },
  { label: "Commit Missed Tickets", value: "commit_missed_tickets" },
  { label: "Done Tickets", value: "done_tickets" },
  { label: "Missed Tickets", value: "missed_tickets" },
  /**
   * Averages
   */
  { label: "Average Ticket Size Per Sprint", value: spritnMetricKeyTypes.AVG_TICKET_SIZE_PER_SPRINT },
  { label: "Average Churn Rate", value: sprintReportDataKeyTypes.AVG_CHURN_RATE }
];

export const sprintMetricUnitKeys = Object.values(sprintReportDataKeyTypes);
export const sprintMetricKeys = statSprintMetricsOptions.map((option: sprintMetricOptionType) => option.value);

export const NotTimePeriodSupportedReports = [
  "resolution_time_counts_stat",
  "azure_resolution_time_counts_stat",
  "jobs_commits_lead_single_stat_report"
];
