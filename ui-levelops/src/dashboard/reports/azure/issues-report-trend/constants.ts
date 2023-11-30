import { chartProps } from "../constant";

export const REPORT_NAME = "Issues Trend Report";
export const COMPOSITE_TRANSFORM = {
  total_tickets: "total_jira_tickets"
};

export const CHART_PROPS = {
  unit: "Tickets",
  chartProps: chartProps
};

export const FILTERS = {
  across: "trend"
};

export const URI = "issue_management_tickets_report";
