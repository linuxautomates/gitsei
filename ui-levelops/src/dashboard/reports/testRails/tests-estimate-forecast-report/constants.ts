import { chartProps } from "dashboard/reports/commonReports.constants";

export const testRailsTestsEstimateForecastReportChartProps = {
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
