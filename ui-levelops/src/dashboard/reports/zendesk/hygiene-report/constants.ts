import { BASE_ZENDESK_CHART_PROPS } from "../constant";

export const HygieneTypes = ["IDLE", "POOR_DESCRIPTION", "NO_CONTACT", "MISSED_RESOLUTION_TIME"];

export const ZENDESK_HYGIENE_REPORT_CHART_PROPS = {
  unit: "tickets",
  barProps: [
    {
      name: "total_tickets",
      dataKey: "total_tickets"
    }
  ],
  stacked: false,
  sortBy: "total_tickets",
  chartProps: BASE_ZENDESK_CHART_PROPS
};

export const ZENDESK_HYGIENE_TYPES = ["IDLE", "POOR_DESCRIPTION", "NO_CONTACT", "MISSED_RESOLUTION_TIME"];
