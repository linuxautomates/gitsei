import { azureBAReportTypes } from "dashboard/constants/enums/azure-ba-reports.enum";
import {
  AZURE_LEAD_TIME_ISSUE_REPORT,
  DEV_PRODUCTIVITY_REPORTS,
  DORA_REPORTS,
  ISSUE_MANAGEMENT_REPORTS,
  LEAD_MTTR_DORA_REPORTS,
  LEAD_TIME_REPORTS
} from "../../../dashboard/constants/applications/names";
import { jiraBAReportTypes } from "../../../dashboard/constants/enums/jira-ba-reports.enum";

export const DisabledPreviewWidgets = [
  LEAD_TIME_REPORTS.JIRA_LEAD_TIME_BY_TYPE_REPORT,
  LEAD_TIME_REPORTS.JIRA_LEAD_TIME_BY_STAGE_REPORT,
  LEAD_TIME_REPORTS.SCM_PR_LEAD_TIME_BY_STAGE_REPORT,
  LEAD_TIME_REPORTS.LEAD_TIME_BY_TIME_SPENT_IN_STAGES_REPORT,
  AZURE_LEAD_TIME_ISSUE_REPORT.ISSUE_LEAD_Time_BY_STAGE_REPORT,
  AZURE_LEAD_TIME_ISSUE_REPORT.ISSUE_LEAD_TIME_BY_TYPE_REPORT,
  jiraBAReportTypes.EFFORT_INVESTMENT_TREND_REPORT,
  ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_TREND_REPORT,
  ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_SINGLE_STAT_REPORT,
  jiraBAReportTypes.EFFORT_INVESTMENT_SINGLE_STAT,
  jiraBAReportTypes.JIRA_EFFORT_INVESTMENT_ENGINEER_REPORT,
  ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_ENGINEER_REPORT,
  jiraBAReportTypes.JIRA_EI_ALIGNMENT_REPORT,
  ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_ALIGNMENT_REPORT,
  "review_collaboration_report",
  DEV_PRODUCTIVITY_REPORTS.PRODUCTIVITY_SCORE_BY_ORG_UNIT,
  DEV_PRODUCTIVITY_REPORTS.DEV_PRODUCTIVITY_SCORE_REPORT,
  DEV_PRODUCTIVITY_REPORTS.DEV_PRODUCTIVITY_PR_ACTIVITY_REPORT,
  DEV_PRODUCTIVITY_REPORTS.INDIVIDUAL_RAW_STATS,
  DEV_PRODUCTIVITY_REPORTS.ORG_RAW_STATS,
  azureBAReportTypes.AZURE_PROGRAM_PROGRESS_REPORT,
  DORA_REPORTS.DEPLOYMENT_FREQUENCY_REPORT,
  DORA_REPORTS.CHANGE_FAILURE_RATE,
  DORA_REPORTS.LEADTIME_CHANGES,
  DORA_REPORTS.MEANTIME_RESTORE,
  LEAD_MTTR_DORA_REPORTS.LEAD_TIME_FOR_CHANGE,
  LEAD_MTTR_DORA_REPORTS.MEAN_TIME_TO_RESTORE
];
