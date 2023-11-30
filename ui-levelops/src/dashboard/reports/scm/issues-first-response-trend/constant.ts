import { BASE_SCM_CHART_PROPS } from "../constant";

export const ACROSS_OPTIONS = ["issue_created", "issue_updated", "issue_closed", "first_comment"].map(
  (item: string) => ({
    label: item.replace(/_/g, " ")?.toUpperCase(),
    value: item
  })
);

export const SCM_FIRST_RESPONSE_TREND_CHART_PROPS = {
  unit: "Days",
  chartProps: BASE_SCM_CHART_PROPS
};

export const SCM_FIRST_RESPONSE_TREND_API_BASED_FILTERS = ["creators"];
