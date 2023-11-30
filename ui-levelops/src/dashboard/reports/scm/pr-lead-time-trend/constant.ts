import { BASE_SCM_CHART_PROPS } from "../constant";

export const REPORT_FILTERS = {
  across: "trend",
  calculation: "pr_velocity"
};

export const SCM_LEAD_TIME_TREND_CHART_PROPS = {
  unit: "Days",
  stackedArea: true,
  chartProps: {
    ...BASE_SCM_CHART_PROPS,
    margin: { top: 20, right: 5, left: 5, bottom: 20 }
  }
};

export const SCM_LEAD_TIME_DEFAULT_QUERY = {
  limit_to_only_applicable_data: false
};

export const SCM_LEAD_TIME_API_BASED_FILTERS = ["creators", "committers", "authors", "assignees", "reviewers"];

export const SCM_LEAD_TIME_TREND_FIELD_KEY_MAPPING = {
  creators: "creator",
  reviewers: "reviewer",
  assignees: "assignee"
};
