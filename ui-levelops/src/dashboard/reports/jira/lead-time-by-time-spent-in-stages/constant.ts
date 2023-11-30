import { chartProps } from "dashboard/reports/commonReports.constants";
import moment from "moment";

export const LEAD_TIME_BY_TIME_SPENT_CHART_PROPS = {
  unit: "Days",
  showStaticLegends: true,
  chartProps
};

export const LEAD_TIME_BY_TIME_SPENT_API_BASED_FILTERS = ["reporters", "assignees"];

export const STAGE_DURATION_FILTERS_OPTIONS = [
  { value: "p90", label: "90th percentile time in stage" },
  { value: "p95", label: "95th percentile time in stage" },
  { value: "mean", label: "Average time in stage" },
  { value: "median", label: "Median time in stage" }
];

export const LEAD_TIME_BY_TIME_SPENT_DEFAULT_QUERY = {
  issue_resolved_at: {
    $gt: moment.utc().subtract(3, "weeks").startOf("week").unix().toString(),
    $lt: moment.utc().unix().toString()
  },
  released_in: {
    $gt: moment.utc().subtract(3, "weeks").startOf("week").unix().toString(),
    $lt: moment.utc().unix().toString()
  }
};

export const LEAD_TIME_BY_TIME_SPENT_DESCRIPTION =
  "The amount of time spent by an issue in a given stage. This widget helps identify issues with bottlenecks.";

export const CUSTOM_FIELD_KEY = "custom_fields";

export const requiredOneFiltersKeys = ["issue_resolved_at", "released_in"];
