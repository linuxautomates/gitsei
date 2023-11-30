import { chartProps } from "dashboard/reports/commonReports.constants";

export const pagerdutyResponseTimeReportChartProps = {
  unit: "Hours",
  sortBy: "from",
  chartProps: chartProps
};

export const pagerdutyResponseTimeReportFilter = {
  integration_type: "PAGERDUTY"
};
