import { DEV_PRODUCTIVITY_REPORTS, ISSUE_MANAGEMENT_REPORTS } from "dashboard/constants/applications/names";
import {
  ACTIVE_WORK_UNIT_FILTER_KEY,
  TICKET_CATEGORIZATION_UNIT_FILTER_KEY
} from "dashboard/constants/bussiness-alignment-applications/constants";
import { jiraBAReportTypes } from "dashboard/constants/enums/jira-ba-reports.enum";

/** it is used to include "interval" filter for the widget filter count  */
export const includeIntervalForWidgetFilterCount = [
  jiraBAReportTypes.EFFORT_INVESTMENT_TREND_REPORT,
  ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_TREND_REPORT,
  DEV_PRODUCTIVITY_REPORTS.DEV_PRODUCTIVITY_SCORE_REPORT,
  DEV_PRODUCTIVITY_REPORTS.PRODUCTIVITY_SCORE_BY_ORG_UNIT,
  DEV_PRODUCTIVITY_REPORTS.DEV_PRODUCTIVITY_PR_ACTIVITY_REPORT
];

export const WIDGET_FILTER_PREVIEW_IGNORE_KEYS: string[] = [
  "across",
  "metric",
  "metrics",
  "stacks",
  "custom_stacks",
  "across_limit",
  "interval",
  "calculation",
  "filter_across_values",
  TICKET_CATEGORIZATION_UNIT_FILTER_KEY
];

/** These keys are ignored for widget filters preview as normal filters */
export const WIDGET_FILTER_PREVIEW_NORMAL_FILTERS_IGNORE_KEYS: string[] = [
  ...WIDGET_FILTER_PREVIEW_IGNORE_KEYS,
  "poor_description",
  "idle",
  ACTIVE_WORK_UNIT_FILTER_KEY
];

/** These keys are ignored for widget filters preview as api filters */
export const WIDGET_FILTER_PREVIEW_API_FILTERS_IGNORE_KEYS: string[] = [...WIDGET_FILTER_PREVIEW_IGNORE_KEYS];

/** These keys are ignored for widget filters preview for github prs committers filters */
export const GITHUB_PRS_COMMITTERS_IGNORE_KEYS: string[] = [...WIDGET_FILTER_PREVIEW_IGNORE_KEYS];
