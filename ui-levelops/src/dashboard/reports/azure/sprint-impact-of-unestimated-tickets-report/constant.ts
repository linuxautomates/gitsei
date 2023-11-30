import { completedDateOptions } from "dashboard/graph-filters/components/Constants";
import { modificationMappedValues } from "dashboard/graph-filters/components/helper";
import { sprintMetricsChartColors } from "shared-resources/charts/chart-themes";

export const METRIC_OPTIONS = [{ value: "avg_commit_to_done", label: "Avg. Done to Commit" }];

export const CHART_PROPS = {
  customColors: sprintMetricsChartColors,
  showNegativeValuesAsPositive: true,
  hideTotalInTooltip: true,
  stacked: true,
  hideGrid: true,
  chartProps: {
    barGap: 0,
    margin: { top: 20, right: 5, left: 5, bottom: 50 },
    stackOffset: "sign",
    barProps: [
      {
        name: "Missed Points",
        dataKey: "missed_points",
        unit: "Points"
      }
    ],
    legendProps: {
      align: "left",
      iconType: "circle",
      verticalAlign: "bottom"
    },
    className: "sprint_impact_estimated_bar_chart"
  },
  unit: "Points"
};

export const DEFAULT_QUERY = {
  completed_at: modificationMappedValues("last_month", completedDateOptions),
  view_by: "Points"
};

export const REPORT_NAME = "Sprint Impact of Unestimated Tickets Report";
export const FILTERS = {
  include_workitem_ids: true
};
export const DEFAULT_ACROSS = "week";
export const URI = "issue_management_sprint_report";
