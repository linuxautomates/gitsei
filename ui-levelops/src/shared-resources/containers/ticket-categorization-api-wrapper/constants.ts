import { azureBAReportTypes } from "dashboard/constants/enums/azure-ba-reports.enum";
import { jiraBAReportTypes } from "dashboard/constants/enums/jira-ba-reports.enum";

export const OPEN_REPORTS_WITHOUT_DRILLDOWN = [
  jiraBAReportTypes.JIRA_PROGRESS_REPORT,
  jiraBAReportTypes.EPIC_PRIORITY_TREND_REPORT,
  azureBAReportTypes.AZURE_ISSUES_PROGRESS_REPORT,
  azureBAReportTypes.AZURE_PROGRAM_PROGRESS_REPORT
];
