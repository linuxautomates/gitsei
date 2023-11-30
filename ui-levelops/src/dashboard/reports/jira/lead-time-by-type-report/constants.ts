import { chartProps } from "dashboard/reports/commonReports.constants";

export const leadTimeByTypeReportChartProps = {
  unit: "Days",
  chartProps: chartProps
};

export const leadTimeByTypeReportFilter = {
  calculation: "ticket_velocity",
  stacks: ["issue_type"]
};
