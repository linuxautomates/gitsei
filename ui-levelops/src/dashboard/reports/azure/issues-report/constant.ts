import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { CHART_TOOLTIP_RENDER_TRANSFORM } from "dashboard/constants/applications/names";
import { WidgetFilterType } from "dashboard/constants/enums/WidgetFilterType.enum";
import { IssueVisualizationTypes } from "dashboard/constants/typeConstants";
import { FilterConfigBasedPreviewFilterConfigType } from "model/report/baseReport.constant";
import { AZURE_APPEND_ACROSS_OPTIONS, chartProps } from "../constant";
import { AzureIssuesReportMetricTypes } from "./enums";
import { azureIssuesChartTooltipTransformer } from "./helper";

export const ACROSS_OPTIONS = [
  { label: "ASSIGNEE", value: "assignee" },
  { label: "Azure Areas", value: "code_area" },
  { label: "Azure Iteration", value: "sprint" },
  { label: "AZURE TEAMS", value: "teams" },
  { label: "WORKITEM CREATED BY MONTH", value: "workitem_created_at_month" },
  { label: "WORKITEM CREATED BY QUARTER", value: "workitem_created_at_quarter" },
  { label: "WORKITEM CREATED BY WEEK", value: "workitem_created_at_week" },
  { label: "WORKITEM RESOLVED BY MONTH", value: "workitem_resolved_at_month" },
  { label: "WORKITEM RESOLVED BY QUARTER", value: "workitem_resolved_at_quarter" },
  { label: "WORKITEM RESOLVED BY WEEK", value: "workitem_resolved_at_week" },
  { label: "WORKITEM UPDATED BY MONTH", value: "workitem_updated_at_month" },
  { label: "WORKITEM UPDATED BY QUARTER", value: "workitem_updated_at_quarter" },
  { label: "WORKITEM UPDATED BY WEEK", value: "workitem_updated_at_week" },
  { label: "PRIORITY", value: "priority" },
  { label: "PROJECT", value: "project" },
  { label: "REPORTER", value: "reporter" },
  { label: "STATUS", value: "status" },
  { label: "TICKET CATEGORY", value: "ticket_category" },
  { label: "TREND", value: "trend" },
  { label: "WORKITEM TYPE", value: "workitem_type" },
  { label: "Azure Story Points", value: "story_points" },
  { label: "Features", value: "parent_workitem_id" }
];

export const STACK_OPTIONS = [
  { label: "project", value: "project" },
  { label: "status", value: "status" },
  { label: "priority", value: "priority" },
  { label: "workitem type", value: "workitem_type" },
  { label: "status category", value: "status_category" },
  { label: "parent workitem id", value: "parent_workitem_id" },
  { label: "epic", value: "epic" },
  { label: "assignee", value: "assignee" },
  { label: "ticket category", value: "ticket_category" },
  { label: "version", value: "version" },
  { label: "fix version", value: "fix_version" },
  { label: "reporter", value: "reporter" },
  { label: "label", value: "label" },
  { label: "Azure Story points", value: "story_points" },
  { label: "teams", value: "teams" },
  { label: "code area", value: "code_area" }
];

export const METRIC_OPTIONS = [
  { value: "ticket", label: "Number of tickets" },
  { value: "story_point", label: "Sum of story points" },
  { value: "effort", label: "Sum of effort" }
];
export const METRIC_URI_MAPPING = "METRIC_URI_MAPPING";

export const ticketsReportVisulizationFilter = {
  type: WidgetFilterType.DROPDOWN_BASED_FILTERS,
  label: "Visualization",
  BE_key: "visualization",
  configTab: WIDGET_CONFIGURATION_KEYS.SETTINGS,
  options: [
    { label: "Bar Chart", value: IssueVisualizationTypes.BAR_CHART },
    { label: "Donut Chart", value: IssueVisualizationTypes.DONUT_CHART },
    { label: "Percentage Stacked Bar Chart", value: IssueVisualizationTypes.PERCENTAGE_STACKED_BAR_CHART }
  ],
  defaultValue: IssueVisualizationTypes.BAR_CHART,
  optionsTransformFn: (data: any) => {
    const { filters } = data;
    if (!filters?.stacks?.length || filters?.stacks[0] === undefined) {
      return [
        { label: "Bar Chart", value: IssueVisualizationTypes.BAR_CHART },
        { label: "Donut Chart", value: IssueVisualizationTypes.DONUT_CHART }
      ];
    }
    return [
      { label: "Bar Chart", value: IssueVisualizationTypes.BAR_CHART },
      { label: "Donut Chart", value: IssueVisualizationTypes.DONUT_CHART },
      { label: "Percentage Stacked Bar Chart", value: IssueVisualizationTypes.PERCENTAGE_STACKED_BAR_CHART }
    ];
  }
};

export const TOTAL_SUM_OF_STORY_POINTS = "Total sum of story points";

export const TOTAL_NUMBER_OF_TICKETS = "Total number of tickets";

export const TOTAL_EFFORT = "Total effort";

export const azureIssuesReportMetricsChartMapping: { [x in AzureIssuesReportMetricTypes]: string } = {
  [AzureIssuesReportMetricTypes.TOTAL_TICKETS]: "Total tickets",
  [AzureIssuesReportMetricTypes.TOTAL_STORY_POINTS]: "Total story points",
  [AzureIssuesReportMetricTypes.TOTAL_EFFORT]: TOTAL_EFFORT
};

export const CHART_PROPS = {
  barProps: [
    {
      name: "total_tickets",
      dataKey: "total_tickets",
      unit: "Tickets"
    }
  ],
  stacked: false,
  unit: "Tickets",
  sortBy: "total_tickets",
  chartProps: chartProps
};

export const APPEND_ACROSS_OPTIONS = [
  ...AZURE_APPEND_ACROSS_OPTIONS,
  { label: "Azure Iteration", value: "sprint" },
  { label: "Ticket Category", value: "ticket_category" }
];

export const DEFAULT_QUERY = {
  metric: ["ticket"]
};

export const REPORT_NAME = "Issues Report";
export const DEFAULT_ACROSS = "assignee";
export const URI = "issue_management_tickets_report";
export const STORY_POINT_URI = "issue_management_story_point_report";
export const EFFORT_URI = "issue_management_effort_report";

export const INFORMATION_MESSAGE = {
  stacks_disabled: "Stacks option is not applicable for Donut visualization"
};

export const FILTERS_KEY_MAPPING = {
  ticket_categories: "workitem_ticket_categories",
  ticket_categorization_scheme: "workitem_ticket_categorization_scheme"
};

export const CHART_DATA_TRANSFORMATION = {
  [CHART_TOOLTIP_RENDER_TRANSFORM]: azureIssuesChartTooltipTransformer
};

export const METRIC_URI_MAPPING_LIST = {
  ticket: URI,
  story_point: STORY_POINT_URI,
  effort: EFFORT_URI
};
export const METRIC_VALUE_KEY_MAPPING: Record<string, string> = {
  ticket: "total_tickets",
  story_point: "total_story_points",
  effort: "total_effort"
};

export const ACROSS_FILTER_LABEL_MAPPING = { parent_workitem_id: "Features" };
export const FILTER_CONFIG_BASED_PREVIEW_FILTERS: FilterConfigBasedPreviewFilterConfigType[] = [
  { filter_key: "workitem_feature", valueKey: "workitem_id", labelKey: "summary" },
  { filter_key: "workitem_user_story", valueKey: "workitem_id", labelKey: "summary" }
];
