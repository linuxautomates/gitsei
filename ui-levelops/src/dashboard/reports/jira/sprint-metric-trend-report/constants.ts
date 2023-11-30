import moment from "moment";
import { jiraChartProps } from "../commonJiraReports.constants";

export const sprintMetricTrendReportColumnInfo = {
  additional_key: "Sprint name",
  status: "Issue status at sprint close.",
  story_points: "Story points at sprint start and close."
};

export const sprintMetricsTrendsChartColors = ["#A5C980", "#4197FF", "#CB98F3", "#FF4D4F"];

export const sprintMetricTrendChartProps = {
  customColors: sprintMetricsTrendsChartColors,
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
    ...jiraChartProps,
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

export const sprintMetricsTrendReportDefaultQuery = {
  agg_type: "average", // transformer use it for average calculation
  completed_at: {
    // required filters and default is last month
    $gt: moment.utc().subtract(30, "days").startOf("day").unix().toString(),
    $lt: moment.utc().unix().toString()
  },
  interval: "week",
  metric: ["creep_done_points", "commit_done_points", "commit_not_done_points", "creep_not_done_points"],
  view_by: "Points"
};

export const METRICS_OPTIONS: { label: string; value: string }[] = [
  { value: "commit_not_done_points", label: "Commit missed / over done" },
  { value: "commit_done_points", label: "Commit done" },
  { value: "creep_done_points", label: "Creep done" },
  { value: "creep_not_done_points", label: "Creep missed / over done" }
];

export const FILTERS_TO_HIDE = ["statuses", "status_categories"];
