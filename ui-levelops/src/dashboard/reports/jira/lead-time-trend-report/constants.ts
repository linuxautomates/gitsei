import { chartProps } from "dashboard/reports/commonReports.constants";

export const leadTimeTrendReportChartProps = {
  unit: "Days",
  stackedArea: true,
  chartProps: {
    ...chartProps,
    margin: { top: 20, right: 5, left: 5, bottom: 20 }
  }
};

export const leadTimeTrendReportFilter = {
  across: "trend",
  calculation: "ticket_velocity"
};
