import { chartProps } from "dashboard/reports/commonReports.constants";

export const LEAD_TIME_BY_STAGE_REPORT_CHART_PROPS = {
  unit: "Days",
  chartProps: chartProps,
  showStaticLegends: true
};

export const LEAD_TIME_BY_STAGE_REPORT_FILTER = {
  calculation: "ticket_velocity"
};

export const LEAD_TIME_STAGE_REPORT_DESCRIPTION =
  "The amount of time involved from the first commit to getting into production is depicted by splitting into all the involved stages. It helps in identifying the bottlenecks by displaying if each of the stages is in a good, acceptable or slow state as per the threshold defined.";

export const LEAD_TIME_STAGE_DEFAULT_QUERY = {
  limit_to_only_applicable_data: true,
  ratings: ["good", "slow", "needs_attention"]
};

export const DEFAULT_LABEL = "Show tickets with missing stages";
