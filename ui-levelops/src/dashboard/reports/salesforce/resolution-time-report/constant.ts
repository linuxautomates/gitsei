import { BASE_SALESFORCE_CHART_PROPS } from "../constant";

export const SALESFORCE_RESOLUTION_TIME_CHART_PROPS = {
  barProps: [
    {
      name: "min",
      dataKey: "min"
    },
    {
      name: "median",
      dataKey: "median"
    },
    {
      name: "max",
      dataKey: "max"
    }
  ],
  stacked: false,
  unit: "Days",
  sortBy: "median",
  chartProps: BASE_SALESFORCE_CHART_PROPS
};
