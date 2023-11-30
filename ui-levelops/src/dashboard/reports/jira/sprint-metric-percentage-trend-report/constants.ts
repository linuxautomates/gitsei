import moment from "moment";
import { sprintMetricsPercentageColors } from "shared-resources/charts/chart-themes";
import { jiraChartProps } from "../commonJiraReports.constants";

export const sprintMetricPerTrendReportColumnsWithInfo = {
  additional_key: "Sprint name",
  status: "Issue status at sprint close.",
  story_points: "Story points at sprint start and close."
};

export const sprintMetricPerTrendChartProps = {
  customColors: sprintMetricsPercentageColors,
  unit: "Percentage",
  chartProps: jiraChartProps,
  areaProps: [
    {
      name: "Creep to Commit",
      dataKey: "creep_to_commit_ratio",
      unit: "%",
      transformer: (data: string | number) => data + " %"
    },
    {
      name: "Delivered to Commit",
      dataKey: "done_to_commit_ratio",
      unit: "%",
      transformer: (data: string | number) => data + " %"
    },
    {
      name: "Creep Done to Commit",
      dataKey: "creep_done_to_commit_ratio",
      unit: "%",
      transformer: (data: string | number) => data + " %"
    },
    {
      name: "Creep Done",
      dataKey: "creep_done_ratio",
      unit: "%",
      transformer: (data: string | number) => data + " %"
    },
    {
      name: "Creep Missed",
      dataKey: "creep_missed_ratio",
      unit: "%",
      transformer: (data: string | number) => data + " %"
    },
    {
      name: "Commit Missed",
      dataKey: "commit_missed_ratio",
      unit: "%",
      transformer: (data: string | number) => data + " %"
    },
    {
      name: "Commit Done",
      dataKey: "commit_done_ratio",
      unit: "%",
      transformer: (data: string | number) => data + " %"
    }
  ],
  stackedArea: false,
  showGrid: true,
  showDots: true,
  fillOpacity: 0.2,
  legendType: "circle",
  areaType: "linear",
  showTotalOnTooltip: true
};

export const sprintMetricsPercentageReport = {
  agg_type: "average", // transformer use it for average calculation
  completed_at: {
    // required filters and default is last month
    $gt: moment.utc().subtract(30, "days").startOf("day").unix().toString(),
    $lt: moment.utc().unix().toString()
  },
  interval: "week",
  metric: ["commit_done_ratio", "creep_done_to_commit_ratio"],
  view_by: "Points"
};

export const METRICS_OPTIONS: { label: string; value: string }[] = [
  { value: "done_to_commit_ratio", label: "Delivered to Commit Ratio" },
  { value: "creep_to_commit_ratio", label: "Creep to Commit Ratio" },
  { value: "creep_done_to_commit_ratio", label: "Creep Done to Commit Ratio" },
  { value: "creep_done_ratio", label: "Creep Done Ratio" },
  { value: "creep_missed_ratio", label: "Creep Missed Ratio" },
  { value: "commit_missed_ratio", label: "Commit Missed Ratio" },
  { value: "commit_done_ratio", label: "Commit Done Ratio" }
];
