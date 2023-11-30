import { chartProps } from "dashboard/reports/commonReports.constants";

export const pagerdutyAlertReportChartProps = {
  unit: "Incidents/Alerts",
  sortBy: "from",
  chartProps: chartProps
};

export const pagerdutyAlertReportFilter = {
  integration_type: "PAGERDUTY"
};
