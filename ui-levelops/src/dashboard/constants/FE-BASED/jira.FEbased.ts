import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import moment from "moment";
import { WidgetFilterType } from "../enums/WidgetFilterType.enum";

// Sprint distribution filters
export const defaultFilter = {
  agg_metric: "story_points",
  completed_at: {
    // required filters and default is last month
    $gt: moment.utc().subtract(30, "days").startOf("day").unix().toString(),
    $lt: moment.utc().unix().toString()
  }
};
export const aggMetric = {
  type: WidgetFilterType.DROPDOWN_BASED_FILTERS,
  label: "METRIC",
  BE_key: "agg_metric",
  configTab: WIDGET_CONFIGURATION_KEYS.METRICS,
  options: [
    { label: "Average of delivered Story Points", value: "story_points" },
    { label: "Average of delivered Ticket Count", value: "ticket_count" }
  ]
};

export const percentile = {
  type: WidgetFilterType.DROPDOWN_BASED_FILTERS,
  label: "Percentile",
  BE_key: "percentiles",
  configTab: WIDGET_CONFIGURATION_KEYS.FILTERS,
  options: [
    { label: "25", value: 25 },
    { label: "50", value: 50 },
    { label: "75", value: 75 },
    { label: "100", value: 100 }
  ],
  select_mode: "multiple"
};
