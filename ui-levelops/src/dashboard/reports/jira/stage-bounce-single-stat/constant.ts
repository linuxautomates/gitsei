import { WidgetFilterType } from "../../../constants/enums/WidgetFilterType.enum";
import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { jiraSupportedFilters } from "../../../constants/supported-filters.constant";
import { statDefaultQuery } from "../../../constants/helper";

export const stageBounceMetric = {
  type: WidgetFilterType.DROPDOWN_BASED_FILTERS,
  label: "Metric",
  BE_key: "metric",
  configTab: WIDGET_CONFIGURATION_KEYS.METRICS,
  defaultValue: "mean",
  options: [
    { value: "mean", label: "Mean Number of Times in stage" },
    { value: "median", label: "Median Number of Times in stage" },
    { value: "total_tickets", label: "Number of tickets" }
  ]
};

export const stageBounceChartProps = {
  unit: "Times"
};

export const stageBounceDefaultQuery = {
  ...statDefaultQuery,
  stages: ["DONE"],
  metric: "mean"
};

export const stageBounceSupportedFilters = {
  ...jiraSupportedFilters,
  values: [...jiraSupportedFilters.values, "stage"]
};

export const stageBounceMetricOptions = [
  { value: "mean", label: "Mean Number of Times in stage" },
  { value: "median", label: "Median Number of Times in stage" },
  { value: "total_tickets", label: "Number of tickets" }
];

export const TIME_PERIOD_OPTIONS = [
  {
    label: "Last day",
    value: 1
  },
  {
    label: "Last 7 days",
    value: 7
  },
  {
    label: "Last 2 Weeks",
    value: 14
  },
  {
    label: "Last 30 days",
    value: 30
  }
];
