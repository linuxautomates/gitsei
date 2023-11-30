import { BASE_SCM_CHART_PROPS } from "../constant";

export const SCM_PRS_FIRST_REVIEW_TO_MERGE_TREND_CHART_PROPS = {
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

export const SCM_PRS_FIRST_REVIEW_TO_MERGE_TREND_COMPOSITE_TRANSFORM = {
  pr_created: "first_review_to_merge_prs_created",
  pr_updated: "first_review_to_merge_prs_updated",
  pr_merged: "first_review_to_merge_prs_merged"
};
