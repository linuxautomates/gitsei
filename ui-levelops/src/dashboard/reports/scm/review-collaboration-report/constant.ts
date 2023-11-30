import { BASE_SCM_CHART_PROPS } from "../constant";

export const SCM_REVIEW_COLLABORATION_CHART_PROPS = {
  unit: "Count",
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

export const SCM_COLLABORATION_DEFAULT_QUERY = {
  missing_fields: {
    pr_merged: true
  }
};

export const SCM_COLLABORATION_API_BASED_FILTERS = ["committers", "authors", "creators"];
