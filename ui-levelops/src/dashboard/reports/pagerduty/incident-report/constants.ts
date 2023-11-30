import { chartProps } from "dashboard/reports/commonReports.constants";

export const pagerdutyIncidentReportChartProps = {
  unit: "Incidents/Alerts",
  sortBy: "from",
  chartProps: chartProps
};

export const pagerdutyIncidentReportFilter = {
  integration_type: "PAGERDUTY"
};
