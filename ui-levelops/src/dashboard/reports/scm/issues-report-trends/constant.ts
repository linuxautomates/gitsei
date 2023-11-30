import { BASE_SCM_CHART_PROPS } from "../constant";

export const ACROSS_OPTIONS = ["issue_created", "issue_updated", "issue_closed", "first_comment"].map(
  (item: string) => ({
    label: item.replace(/_/g, " ")?.toUpperCase(),
    value: item
  })
);

export const SCM_ISSUES_REPORT_TRENDS_CHART_PROPS = {
  unit: "Count",
  chartProps: BASE_SCM_CHART_PROPS
};

export const SCM_ISSUES_TREND_API_BASED_FILTERS = ["creators", "committers", "authors", "assignees", "reviewers"];
