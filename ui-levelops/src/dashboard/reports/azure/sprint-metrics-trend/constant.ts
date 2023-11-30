import moment from "moment";
import { sprintMetricsChartColors } from "shared-resources/charts/chart-themes";

export const METRICS_OPTIONS = [
  { value: "commit_not_done_points", label: "Commit missed / over done" },
  { value: "commit_done_points", label: "Commit done" },
  { value: "creep_done_points", label: "Creep done" },
  { value: "creep_not_done_points", label: "Creep missed / over done" }
];

export const ACROSS_OPTIONS = [
  { label: "Weekly by Sprint end date", value: "week" },
  { label: "Bi-weekly by Sprint end date", value: "bi_week" },
  { label: "Monthly", value: "month" },
  { label: "Sprint", value: "sprint" }
];

export const CHART_PROPS = {
  customColors: sprintMetricsChartColors,
  showNegativeValuesAsPositive: true,
  hideTotalInTooltip: true,
  barProps: [
    {
      name: "Commit done",
      dataKey: "commit_done_points",
      unit: "Points"
    },
    {
      name: "Commit missed",
      dataKey: "commit_not_done_points",
      unit: "Points"
    },
    {
      name: "Commit over done",
      dataKey: "commit_over_done_points",
      unit: "Points"
    },
    {
      name: "Creep done",
      dataKey: "creep_done_points",
      unit: "Points"
    },
    {
      name: "Creep missed",
      dataKey: "creep_not_done_points",
      unit: "Points"
    },
    {
      name: "Creep over done",
      dataKey: "creep_over_done_points",
      unit: "Points"
    }
  ],
  stacked: true,
  chartProps: {
    barGap: 0,
    margin: { top: 20, right: 5, left: 5, bottom: 50 },
    stackOffset: "sign"
  },
  unit: "Points",
  xAxisProps: {
    interval: "preserveStartEnd",
    ["XAXIS_TRUNCATE_LENGTH"]: 20
  },
  config: {
    showXAxisTooltip: true
  }
};

export const DEFAULT_QUERY = {
  completed_at: {
    $gt: moment.utc().subtract(30, "days").startOf("day").unix().toString(),
    $lt: moment.utc().unix().toString()
  },
  interval: "week",
  metric: ["creep_done_points", "commit_done_points", "commit_not_done_points", "creep_not_done_points"],
  view_by: "Points"
};

export const REPORT_NAME = "Sprint Metrics Trend Report";
export const URI = "issue_management_sprint_report";
export const DEFAULT_ACROSS = "sprint";
export const COLUMNS_WITH_INFO = {
  additional_key: "Sprint name",
  status: "Issue status at sprint close.",
  story_points: "Story points at sprint start and close."
};
export const COMPARE_FIELD = "delivered_story_points";
export const BAR_CHART_STROKE_COLOR = "#4f4f4f";
