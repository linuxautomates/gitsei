import { jiraCommonFilterOptionsMapping, WIDGET_DATA_SORT_FILTER_KEY } from "dashboard/constants/filter-name.mapping";
import { IssueVisualizationTypes } from "dashboard/constants/typeConstants";
import {
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "dashboard/constants/WidgetDataSortingFilter.constant";
import { jiraChartProps } from "../commonJiraReports.constants";

export const JIRA_TICKET_REPORT_CHART_PROPS = {
  barProps: [
    {
      name: "total_tickets",
      dataKey: "total_tickets",
      unit: "Tickets"
    }
  ],
  pieProps: {
    cx: "50%",
    innerRadius: 70,
    outerRadius: 110
  },
  stacked: false,
  unit: "Tickets",
  sortBy: "total_tickets",
  chartProps: jiraChartProps,
  xAxisProps: {
    ["XAXIS_TRUNCATE_LENGTH"]: 20
  }
};

export const JIRA_TICKET_REPORT_QUERY = {
  metric: "ticket",
  [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED],
  visualization: IssueVisualizationTypes.BAR_CHART
};

export const JIRA_TICKET_REPORT_FILTER_OPTION_MAP = {
  ...jiraCommonFilterOptionsMapping,
  ticket_category: "Ticket Category"
};

export const JIRA_TICKET_REPORT_INFO_MESSAGE = {
  stacks_disabled: "Stacks option is not applicable for Donut visualization"
};

export const ACROSS_OPTIONS = [
  { label: "ASSIGNEE", value: "assignee" },
  { label: "COMPONENT", value: "component" },
  { label: "EPIC", value: "epic" },
  { label: "FIX VERSION", value: "fix_version" },
  { label: "ISSUE CREATED BY MONTH", value: "issue_created_month" },
  { label: "ISSUE CREATED BY QUARTER", value: "issue_created_quarter" },
  { label: "ISSUE CREATED BY WEEK", value: "issue_created_week" },
  { label: "ISSUE DUE DATE BY MONTH", value: "issue_due_month" },
  { label: "ISSUE DUE DATE BY QUARTER", value: "issue_due_quarter" },
  { label: "ISSUE DUE DATE BY WEEK", value: "issue_due_week" },
  { label: "ISSUE RESOLVED BY MONTH", value: "issue_resolved_month" },
  { label: "ISSUE RESOLVED BY QUARTER", value: "issue_resolved_quarter" },
  { label: "ISSUE RESOLVED BY WEEK", value: "issue_resolved_week" },
  { label: "ISSUE TYPE", value: "issue_type" },
  { label: "ISSUE UPDATED BY MONTH", value: "issue_updated_month" },
  { label: "ISSUE UPDATED BY QUARTER", value: "issue_updated_quarter" },
  { label: "ISSUE UPDATED BY WEEK", value: "issue_updated_week" },
  { label: "LABEL", value: "label" },
  { label: "PRIORITY", value: "priority" },
  { label: "PROJECT", value: "project" },
  { label: "REPORTER", value: "reporter" },
  { label: "RESOLUTION", value: "resolution" },
  { label: "STATUS", value: "status" },
  { label: "STATUS CATEGORY", value: "status_category" },
  { label: "TICKET", value: "parent" },
  { label: "TICKET CATEGORY", value: "ticket_category" },
  { label: "Affects Version", value: "version" }
];

export const STACK_OPTIONS = [
  { label: "Affects Version", value: "version" },
  { label: "assignee", value: "assignee" },
  { label: "component", value: "component" },
  { label: "fix_version", value: "fix_version" },
  { label: "issue_type", value: "issue_type" },
  { label: "label", value: "label" },
  { label: "priority", value: "priority" },
  { label: "project", value: "project" },
  { label: "reporter", value: "reporter" },
  { label: "resolution", value: "resolution" },
  { label: "status", value: "status" },
  { label: "status_category", value: "status_category" }
];

export const EPIC_TEXT = "Epic Link";
export const ADD_EXTRA_FILTER = "ADD_EXTRA_FILTER";
export const MAX_STACK_ENTRIES = 99999;
