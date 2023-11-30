import { BASE_SCM_CHART_PROPS } from "../constant";

export const ACROSS_OPTIONS = ["creator", "label", "state", "repo_id", "assignee", "project"].map((item: string) => ({
  label: item.replace(/_/g, " ")?.toUpperCase(),
  value: item
}));

export const SCM_ISSUE_REPORT_CHART_PROPS = {
  unit: "Counts",
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

export const SCM_ISSUES_API_BASED_FILTERS = ["creators", "committers", "authors", "assignees", "reviewers"];
export const SCM_ISSUES_FILTERS_NOT_SUPPORTING_PARTIAL_FILTERS = ["labels", "repo_ids", "assignees"];
