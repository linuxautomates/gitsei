import { WIDGET_DATA_SORT_FILTER_KEY } from "dashboard/constants/filter-name.mapping";
import {
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "dashboard/constants/WidgetDataSortingFilter.constant";
import moment from "moment";
import { formatTooltipValue } from "shared-resources/charts/helper";
import { BASE_SCM_CHART_PROPS } from "../constant";

export const METRIC_OPTIONS = [
  { value: "avg_coding_day_week", label: "Average Coding days per week" },
  { value: "median_coding_day_week", label: "Median Coding days per week" },
  { value: "avg_coding_day_biweekly", label: "Average Coding days per two weeks" },
  { value: "median_coding_day_biweekly", label: "Median Coding days per two weeks" },
  { value: "avg_coding_day_month", label: "Average Coding days per month" },
  { value: "median_coding_day_month", label: "Median Coding days per month" }
];

export const ACROSS_OPTIONS = [
  { label: "Repo ID", value: "repo_id" },
  { label: "Author", value: "author" },
  { label: "Committer", value: "committer" }
];
export const SCM_CODING_DAYS_DESCRIPTION =
  "Average value of all the days where a developer commits code per week for the selected duration.";

export const CODING_DAYS_DEFAULT_QUERY = {
  interval: "week",
  committed_at: {
    $gt: moment.utc().subtract(6, "days").startOf("day").unix().toString(),
    $lt: moment.utc().unix().toString()
  },
  [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED]
};

export const CODING_DAYS_CHART_PROPS = {
  unit: "Days",
  barProps: [
    {
      name: "Average Coding days per week",
      dataKey: "mean",
      unit: "Days"
    }
  ],
  stacked: false,
  barTopValueFormater: (value: number | string) => formatTooltipValue(value),
  chartProps: BASE_SCM_CHART_PROPS
};

export const REPORT_FILTERS = {
  sort: [
    {
      id: "commit_days",
      desc: true
    }
  ]
};
