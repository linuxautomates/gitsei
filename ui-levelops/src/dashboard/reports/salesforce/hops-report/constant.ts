import { BASE_SALESFORCE_CHART_PROPS } from "../constant";

export const SALESFORCE_HOPS_REPORT_CHART_PROPS = {
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
  unit: "Hops",
  sortBy: "median",
  chartProps: BASE_SALESFORCE_CHART_PROPS
};
