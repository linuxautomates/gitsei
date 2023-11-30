import moment from "moment";
import { sprintMetricsPercentageColors } from "shared-resources/charts/chart-themes";

export const VISUALIZATION_OPTIONS = [
  { value: "stacked_area", label: "Stacked Area" },
  { value: "unstacked_area", label: "Unstacked Area" },
  { value: "line", label: "Line" }
];

export const METRICS_OPTIONS = [
  { value: "done_to_commit_ratio", label: "Delivered to Commit Ratio" },
  { value: "creep_to_commit_ratio", label: "Creep to Commit Ratio" },
  { value: "creep_done_to_commit_ratio", label: "Creep Done to Commit Ratio" },
  { value: "creep_done_ratio", label: "Creep Done Ratio" },
  { value: "creep_missed_ratio", label: "Creep Missed Ratio" },
  { value: "commit_missed_ratio", label: "Commit Missed Ratio" },
  { value: "commit_done_ratio", label: "Commit Done Ratio" }
];

export const ACROSS_OPTIONS = [
  { label: "Weekly by Sprint end date", value: "week" },
  { label: "Bi-weekly by Sprint end date", value: "bi_week" },
  { label: "Monthly", value: "month" },
  { label: "Sprint", value: "sprint" }
];

export const DEFAULT_QUERY = {
  completed_at: {
    $gt: moment.utc().subtract(30, "days").startOf("day").unix().toString(),
    $lt: moment.utc().unix().toString()
  },
  interval: "week",
  metric: ["commit_done_ratio", "creep_done_to_commit_ratio"],
  view_by: "Points"
};

export const CHART_PROPS = {
  customColors: sprintMetricsPercentageColors,
  unit: "Percentage",
  chartProps: {
    barGap: 0,
    margin: { top: 20, right: 5, left: 5, bottom: 50 }
  },
  areaProps: [
    {
      name: "Creep to Commit",
      dataKey: "creep_to_commit_ratio",
      unit: "%",
      transformer: (data: any) => data + " %"
    },
    {
      name: "Delivered to Commit",
      dataKey: "done_to_commit_ratio",
      unit: "%",
      transformer: (data: any) => data + " %"
    },
    {
      name: "Creep Done to Commit",
      dataKey: "creep_done_to_commit_ratio",
      unit: "%",
      transformer: (data: any) => data + " %"
    },
    {
      name: "Creep Done",
      dataKey: "creep_done_ratio",
      unit: "%",
      transformer: (data: any) => data + " %"
    },
    {
      name: "Creep Missed",
      dataKey: "creep_missed_ratio",
      unit: "%",
      transformer: (data: any) => data + " %"
    },
    {
      name: "Commit Missed",
      dataKey: "commit_missed_ratio",
      unit: "%",
      transformer: (data: any) => data + " %"
    },
    {
      name: "Commit Done",
      dataKey: "commit_done_ratio",
      unit: "%",
      transformer: (data: any) => data + " %"
    }
  ],
  stackedArea: false,
  showGrid: true,
  showDots: true,
  fillOpacity: 0.2,
  legendType: "circle",
  areaType: "linear",
  showTotalOnTooltip: false
};

export const REPORT_NAME = "Sprint Metrics Percentage Trend Report";
export const URI = "issue_management_sprint_report";
export const DEFAULT_ACROSS = "bi_week";
export const COLUMNS_WITH_INFO = {
  additional_key: "Sprint name",
  status: "Issue status at sprint close.",
  story_points: "Story points at sprint start and close."
};
