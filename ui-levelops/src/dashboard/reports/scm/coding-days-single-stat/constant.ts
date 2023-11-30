import { get } from "lodash";
import moment from "moment";

export const METRIC_OPTIONS = [
  { value: "average_coding_day", label: "Average Coding days" },
  { value: "median_coding_day", label: "Median Coding days" }
];

export const REPORT_FILTERS = {
  across: "committer"
};

export const SCM_CODING_DAYS_STAT_DEFAULT_QUERY = {
  time_period: 1,
  agg_type: "average",
  committed_at: {
    $gt: moment.utc().subtract(6, "days").startOf("day").unix().toString(),
    $lt: moment.utc().unix().toString()
  }
};

export const SCM_CODING_DAYS_STAT_CHART_PROPS = {
  unit: "Days"
};
