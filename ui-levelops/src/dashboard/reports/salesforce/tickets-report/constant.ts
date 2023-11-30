import { BASE_SALESFORCE_CHART_PROPS } from "../constant";

export const SALESFORCE_TICKETS_REPORT = {
  barProps: [
    {
      name: "total_cases",
      dataKey: "total_cases",
      unit: "Tickets"
    }
  ],
  stacked: false,
  unit: "Tickets",
  sortBy: "total_tickets",
  chartProps: BASE_SALESFORCE_CHART_PROPS
};
