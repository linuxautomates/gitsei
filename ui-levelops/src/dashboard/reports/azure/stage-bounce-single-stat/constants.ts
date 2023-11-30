import { issueManagementSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { drillDownValuesToFiltersKeys } from "../constant";

export const DEFAULT_QUERY = {
  time_period: 1,
  agg_type: "average",
  workitem_stages: ["Done"],
  metric: "mean"
};

export const FILTERS = {
  across: "trend"
};

export const SUPPORTED_FILTERS = {
  ...issueManagementSupportedFilters,
  values: [...issueManagementSupportedFilters.values, "workitem_stage"]
};

export const VALUES_TO_FILTERS = {
  ...drillDownValuesToFiltersKeys,
  stage: "workitem_stages",
  workitem_stage: "workitem_stages"
};

export const REPORT_NAME = "Stage Bounce Single Stat";
export const URI = "issue_management_stage_bounce_report";
export const CHART_PROPS = {
  unit: "Times"
};
export const COMPARE_FIELD = "mean";
export const REQUIRED_FILTERS = ["workitem_stage"];
export const SUPPORTED_WIDGET_TYPES = ["stats"];
