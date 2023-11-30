import { BASE_ZENDESK_CHART_PROPS } from "../constant";

export const ZENDESK_REQUESTER_WAIT_TIME_CHART_PROPS = {
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
  chartProps: BASE_ZENDESK_CHART_PROPS
};
