import { BASE_SCM_CHART_PROPS } from "../constant";

export const ACROSS_OPTIONS = [
  { label: "PR Created", value: "pr_created" },
  { label: "PR Updated", value: "pr_updated" },
  { label: "PR Merged", value: "pr_merged" },
  { label: "PR Closed", value: "pr_closed" }
];

export const SCM_PRS_FIRST_REVIEW_TREND_CHART_PROPS = {
  areaProps: [
    {
      name: "min",
      dataKey: "min"
    },
    {
      name: "median",
      dataKey: "median"
    },
    {
      name: "max",
      dataKey: "max"
    }
  ],
  unit: "Hours",
  chartProps: BASE_SCM_CHART_PROPS
};

export const SCM_PRS_FIRST_REVIEW_TREND_COMPOSITE_TRANSFORM = {
  pr_created: "first_review_prs_created",
  pr_updated: "first_review_prs_updated",
  pr_merged: "first_review_prs_merged"
};
