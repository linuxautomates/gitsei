export const METRIC_OPTIONS = [
  { value: "mean", label: "Average time in stage" },
  { value: "median", label: "Median time in stage" },
  { value: "p90", label: "90th percentile time in stage" },
  { value: "p95", label: "95th percentile time in stage" }
];

export const chartProps = {
  barGap: 0,
  margin: { top: 20, right: 5, left: 5, bottom: 20 }
};

export const filters = {
  across: "trend",
  calculation: "ticket_velocity"
};

export const REPORT_NAME = "Issue Lead Time Trend Report";
export const CHART_PROPS = {
  unit: "Days",
  stackedArea: true,
  chartProps: chartProps
};
export const URI = "lead_time_report";
export const DEFAULT_QUERY = {
  limit_to_only_applicable_data: false
};
