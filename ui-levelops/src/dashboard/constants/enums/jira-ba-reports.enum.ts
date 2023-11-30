import { ISSUE_MANAGEMENT_REPORTS, JIRA_MANAGEMENT_TICKET_REPORT } from "../applications/names";

export enum jiraBAReportTypes {
  JIRA_PROGRESS_SINGLE_STAT = "progress_single_stat",
  JIRA_PROGRESS_REPORT = "progress_single_report",
  EFFORT_INVESTMENT_SINGLE_STAT = "effort_investment_single_stat",
  EFFORT_INVESTMENT_TREND_REPORT = "effort_investment_trend_report",
  EPIC_PRIORITY_TREND_REPORT = "epic_priority_trend_report",
  JIRA_BURNDOWN_REPORT = "jira_burndown_report",
  EFFORT_INVESTMENT_TEAM_REPORT = "effort_investment_team_report",
  JIRA_EFFORT_INVESTMENT_ENGINEER_REPORT = "jira_effort_investment_engineer_report",
  JIRA_EI_ALIGNMENT_REPORT = "jira_effort_alignment_report"
}

export const exportableBAReports = [
  jiraBAReportTypes.JIRA_EFFORT_INVESTMENT_ENGINEER_REPORT,
  jiraBAReportTypes.JIRA_EI_ALIGNMENT_REPORT,
  jiraBAReportTypes.EFFORT_INVESTMENT_TREND_REPORT,
  ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_ALIGNMENT_REPORT,
  ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_ENGINEER_REPORT,
  ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_TREND_REPORT,
  JIRA_MANAGEMENT_TICKET_REPORT.JIRA_RELEASE_TABLE_REPORT
];

export const allBAReports = Object.values(jiraBAReportTypes);
// reports with dynamic supported Filters
export const jiraAzureBADynamicSupportedFiltersReports = [
  jiraBAReportTypes.EFFORT_INVESTMENT_SINGLE_STAT,
  jiraBAReportTypes.EFFORT_INVESTMENT_TREND_REPORT,
  jiraBAReportTypes.JIRA_EFFORT_INVESTMENT_ENGINEER_REPORT,
  ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_ENGINEER_REPORT,
  ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_SINGLE_STAT_REPORT,
  ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_TREND_REPORT
];

// jira reports with dynamic supported Filters
export const jiraBADynamicSupportedFiltersReports = [
  jiraBAReportTypes.EFFORT_INVESTMENT_SINGLE_STAT,
  jiraBAReportTypes.EFFORT_INVESTMENT_TREND_REPORT,
  jiraBAReportTypes.JIRA_EFFORT_INVESTMENT_ENGINEER_REPORT
];

export enum IntervalTypeDisplay {
  WEEK = "Week",
  BI_WEEK = "Two Weeks",
  MONTH = "Month",
  QUARTER = "Quarter"
}

export enum IntervalType {
  WEEK = "week",
  BI_WEEK = "biweekly",
  MONTH = "month",
  QUARTER = "quarter"
}

export enum EffortUnitType {
  TICKETS_REPORT = "tickets_report",
  STORY_POINT_REPORT = "story_point_report",
  COMMIT_COUNT = "commit_count_fte",
  TICKET_TIME_SPENT = "effort_investment_time_spent",
  AZURE_TICKETS_REPORT = "azure_effort_investment_tickets",
  AZURE_STORY_POINT_REPORT = "azure_effort_investment_story_point",
  AZURE_COMMIT_COUNT = "azure_effort_investment_commit_count",
  AZURE_TICKET_TIME_SPENT = "azure_effort_investment_time_spent"
}

export enum ActiveEffortUnitType {
  JIRA_TICKETS_COUNT = "active_effort_investment_tickets",
  JIRA_STORY_POINTS = "active_effort_investment_story_points",
  AZURE_TICKETS_COUNT = "active_azure_ei_ticket_count",
  AZURE_STORY_POINTS = "active_azure_ei_story_point"
}

export enum EffortType {
  COMPLETED_EFFORT = "COMPLETED_EFFORT",
  ACTIVE_EFFORT = "ACITVE_EFFORT"
}

export enum allignmentStatus {
  GOOD = "Good",
  ACCEPTABLE = "Acceptable",
  POOR = "Poor"
}
