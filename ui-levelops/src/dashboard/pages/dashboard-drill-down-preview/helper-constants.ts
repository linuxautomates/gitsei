import { IntegrationTypes } from "constants/IntegrationTypes";

export const JIRA_LEAD_TIME_REPORTS = [
  "lead_time_by_type_report",
  "lead_time_trend_report",
  "lead_time_by_stage_report",
  "lead_time_single_stat"
];

// These are the applications that currently supports OU filters
export const APPLICATIONS_SUPPORTING_OU_FILTERS = [
  "azure_devops",
  "jira",
  "jenkins",
  "github",
  "githubjira",
  "jenkinsgithub",
  "sonarqube",
  "any",
  IntegrationTypes.TESTRAILS
];

export const DRILL_DOWN_APPLICATIONS_WITH_THREE_COLUMNS = [
  "jira_velocity",
  "scm_velocity",
  "github_commits",
  "github_prs_first_review",
  "github_prs",
  "dora_lead_mttr"
];
export const DRILL_DOWN_APPLICATIONS_WITH_FOUR_COLUMNS = [
  "jira",
  "praetorian",
  "snyk",
  "azure_devops",
  "github_jira_files",
  "bullseye",
  "github_issues_resolution",
  "jira_assignee_time_report"
];
export const DRILL_DOWN_APPLICATIONS_WITH_FIVE_COLUMNS = [
  "scm_committers",
  "scm_repos",
  "sonarqube",
  "scm_review_collaboration"
];
export const DRILL_DOWN_APPLICATIONS_WITH_SIX_COLUMNS = ["scm_review_collaboration"];
export const MAX_SPRINT_METRICS_UNIT_COLUMNS = 5;
