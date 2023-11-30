import { chartProps } from "dashboard/reports/commonReports.constants";

export const testRailsTestsEstimateReportChartProps = {
  barProps: [
    {
      name: "min",
      dataKey: "min"
    },
    {
      name: "median",
      dataKey: "median"
    },
    {
      name: "max",
      dataKey: "max"
    }
  ],
  stacked: false,
  unit: "Tests",
  sortBy: "median",
  chartProps: chartProps
};
