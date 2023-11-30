import testRailsTestsEstimateForecastReport from "./tests-estimate-forecast-report/report";
import testRailsTestsEstimateForecastTrendReport from "./tests-estimate-forecast-trend-report/report";
import testRailsTestsEstimateReport from "./tests-estimate-report/report";
import testRailsTestsEstimateTrendReport from "./tests-estimate-trend-report/report";
import testRailsTestsReport from "./tests-report/report";
import testRailsTestsTrendReport from "./tests-trend-report/report";

const testRailsReports = {
  ...testRailsTestsReport,
  ...testRailsTestsTrendReport,
  ...testRailsTestsEstimateReport,
  ...testRailsTestsEstimateTrendReport,
  ...testRailsTestsEstimateForecastReport,
  ...testRailsTestsEstimateForecastTrendReport
};

export default testRailsReports;
