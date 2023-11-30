import { BASE_SCM_CHART_PROPS } from "../constant";

export const REPORT_FILTERS = {
  calculation: "pr_velocity"
};

export const SCM_LEAD_TIME_STAGE_DEFAULT_QUERY = {
  limit_to_only_applicable_data: false
};

export const SCM_LEAD_TIME_STAGE_CHART_PROPS = {
  unit: "Days",
  chartProps: BASE_SCM_CHART_PROPS
};

export const SCM_LEAD_TIME_STAGE_API_BASED_FILTERS = ["creators", "committers", "authors", "assignees", "reviewers"];
