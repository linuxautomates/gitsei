import { chartProps } from "dashboard/reports/commonReports.constants";

export const ACROSS_OPTIONS = [
  { label: "Category", value: "category" },
  { label: "Priority", value: "priority" },
  { label: "Tag", value: "tag" },
  { label: "Project", value: "project" }
];

export const STACK_OPTIONS = [
  { label: "Priority", value: "priority" },
  { label: "Tag", value: "tag" },
  { label: "Project", value: "project" }
];

export const praetorianIssuesReportChartTypes = {
  unit: "Issues",
  sortBy: "count",
  chartProps: chartProps,
  barProps: [
    {
      name: "count",
      dataKey: "count"
    }
  ],
  stacked: false
};
