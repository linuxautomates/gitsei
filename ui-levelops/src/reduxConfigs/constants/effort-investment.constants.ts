import { ISSUE_MANAGEMENT_REPORTS } from "dashboard/constants/applications/names";
import { jiraBAReportTypes } from "dashboard/constants/enums/jira-ba-reports.enum";
import { basicMappingType } from "dashboard/dashboard-types/common-types";

export const commitCountCompletedWorkURIs: string[] = [
  "scm_jira_commits_count_ba",
  "azure_effort_investment_commit_count"
];

export enum timeRangeKeys {
  JIRA = "issue_resolved_at",
  AZURE = "workitem_resolved_at",
  SCM = "committed_at"
}

export const storyPointUnit: string[] = ["azure_effort_investment_story_point", "story_point_report"];

export const storyPointUnitToURIMapping: basicMappingType<string> = {
  azure_effort_investment_story_point: "active_azure_ei_story_point",
  story_point_report: "active_effort_investment_story_points"
};

export const activeWorkBasedReports: string[] = [
  jiraBAReportTypes.JIRA_EI_ALIGNMENT_REPORT,
  ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_ALIGNMENT_REPORT
];

export const INTEGRATION_CONFIG_ID_EFFORT_CATEGORIES = "INTEGRATION_CONFIG_ID_EFFORT_CATEGORIES";
