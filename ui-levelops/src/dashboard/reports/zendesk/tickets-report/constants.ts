import { ACROSS_OPTIONS } from "../commonZendeskReports.constants";
import { BASE_ZENDESK_CHART_PROPS } from "../constant";

export const ZENDESK_TICKET_REPORT_ACROSS_OPTIONS = [
  ...ACROSS_OPTIONS,
  { label: "Ticket Created By Date", value: "ticket_created_day" },
  { label: "Ticket Created By Week", value: "ticket_created_week" },
  { label: "Ticket Created By Month", value: "ticket_created_month" }
];

export const ZENDESK_TICKETS_REPORT_CHART_PROPS = {
  barProps: [
    {
      name: "total_tickets",
      dataKey: "total_tickets",
      unit: "Tickets"
    }
  ],
  stacked: false,
  unit: "Tickets",
  sortBy: "total_tickets",
  chartProps: BASE_ZENDESK_CHART_PROPS
};
