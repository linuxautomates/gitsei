import { chartProps } from "dashboard/reports/commonReports.constants";

export const testRailsTestsEstimateTrendReportChartProps = {
  unit: "Tests",
  chartProps: chartProps
};

export const testRailsTestsEstimateTrendReportFilter = {
  across: "trend"
};

export const testRailsTestsEstimateTrendReportCompositeTransform = {
  min: "testrails_tests_estimate_min",
  median: "testrails_tests_estimate_median",
  max: "testrails_tests_estimate_max"
};
