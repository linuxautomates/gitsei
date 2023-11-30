import { BASE_SCM_CHART_PROPS } from "../constant";

export const ACROSS_OPTIONS = [
  { label: "PR Created", value: "pr_created" },
  { label: "PR Updated", value: "pr_updated" },
  { label: "PR Merged", value: "pr_merged" }
];

export const SCM_PRS_MERGE_TREND_CHART_PROPS = {
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
  chartProps: BASE_SCM_CHART_PROPS,
  xAxisProps: {
    ["XAXIS_TRUNCATE_LENGTH"]: 20
  }
};

export const SCM_PRS_MERGE_TREND_COMPOSITE_TRANSFORM = {
  pr_created: "merge_prs_created",
  pr_updated: "merge_prs_updated",
  pr_merged: "merge_prs_merged"
};
