import { BASE_SCM_CHART_PROPS } from "../constant";

export const ACROSS_OPTIONS = ["creator", "label", "state", "repo_id", "assignee", "project"].map((item: string) => ({
  label: item.replace(/_/g, " ")?.toUpperCase(),
  value: item
}));

export const SCM_ISSUES_FIRST_RESPONSE_REPORT_CHART_PROPS = {
  unit: "Days",
  barProps: [
    {
      name: "count",
      dataKey: "count",
      unit: "count"
    }
  ],
  stacked: false,
  chartProps: BASE_SCM_CHART_PROPS
};

export const SCM_ISSUES_FIRST_RESPONSE_API_BASED_FILTERS = [
  "creators",
  "committers",
  "authors",
  "assignees",
  "reviewers"
];
