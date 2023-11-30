import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { CHART_TOOLTIP_RENDER_TRANSFORM } from "dashboard/constants/applications/names";
import { WidgetFilterType } from "dashboard/constants/enums/WidgetFilterType.enum";
import { IssueVisualizationTypes } from "dashboard/constants/typeConstants";
import { AZURE_APPEND_ACROSS_OPTIONS, chartProps } from "../constant";
import { AzureIssuesReportMetricTypes } from "./enums";
import { azureIssuesChartTooltipTransformer } from "./helper";

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
  { label: "Azure Story Points", value: "story_points" },
  { label: "teams", value: "teams" },
  { label: "code area", value: "code_area" }
];

export const METRIC_OPTIONS = [
  { value: "ticket", label: "Number of tickets" },
  { value: "story_point", label: "Sum of story points" }
];

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

export const azureIssuesReportMetricsChartMapping: { [x in AzureIssuesReportMetricTypes]: string } = {
  [AzureIssuesReportMetricTypes.TOTAL_TICKETS]: "Total tickets",
  [AzureIssuesReportMetricTypes.TOTAL_STORY_POINTS]: "Total story points"
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

export const REPORT_NAME = "Issue Progress Report";
export const DEFAULT_ACROSS = "epic";
export const URI = "issue_management_tickets_report";
export const STORY_POINT_URI = "issue_management_story_point_report";
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

export const ACROSS_OPTIONS = [
  { label: "Effort Investment Category", value: "ticket_category" },
  { label: "Epics", value: "epic" }
];
