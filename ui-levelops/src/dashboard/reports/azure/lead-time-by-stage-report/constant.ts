import { chartProps } from "../constant";

export const METRIC_OPTIONS = [
  { value: "mean", label: "Average time in stage" },
  { value: "median", label: "Median time in stage" },
  { value: "p90", label: "90th percentile time in stage" },
  { value: "p95", label: "95th percentile time in stage" }
];

export const REPORT_NAME = "Issue Lead Time by Stage Report";

export const CHART_PROPS = {
  unit: "Days",
  chartProps: chartProps
};

export const DATA_KEY = "duration";
export const URI = "lead_time_report";
export const FILTERS = {
  calculation: "ticket_velocity"
};
export const DEFAULT_ACROSS = "velocity";
export const MIN_WIDTH = "32rem";
export const DEFAULT_QUERY = {
  limit_to_only_applicable_data: true,
  ratings: ["good", "slow", "needs_attention"]
};
