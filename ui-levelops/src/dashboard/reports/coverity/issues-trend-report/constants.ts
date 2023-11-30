import { chartProps } from "dashboard/reports/commonReports.constants";

export const coverutyIssuesTrendReportChartTypes = {
  barProps: [
    {
      name: "total_defects",
      dataKey: "total_defects",
      unit: "Defects"
    }
  ],
  stacked: false,
  unit: "Defects",
  sortBy: "total_defects",
  chartProps: chartProps
};
