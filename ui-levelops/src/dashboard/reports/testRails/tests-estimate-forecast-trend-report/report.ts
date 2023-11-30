import { trendReportTransformer } from "custom-hooks/helpers";
import { testrailsSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { TestsEstimateForecastTrendReportType } from "model/report/testRails/tests-estimate-forecast-trend-report/testsEstimateForecastTrendReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { testRailsDrilldown } from "../commonTestRailsReports.constants";
import {
  testRailsTestsEstimateForecastTrendReportChartProps,
  testRailsTestsEstimateForecastTrendReportCompositeTransform,
  testRailsTestsEstimateForecastTrendReportFilter
} from "./constants";
import { IntegrationTypes } from "constants/IntegrationTypes";

const testRailsTestsEstimateForecastTrendReport: {
  testrails_tests_estimate_forecast_trend_report: TestsEstimateForecastTrendReportType;
} = {
  testrails_tests_estimate_forecast_trend_report: {
    name: "TestRail Test Estimate Forecast Trend Report",
    application: IntegrationTypes.TESTRAILS,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "testrails_tests_estimate_forecast_report",
    method: "list",
    filters: testRailsTestsEstimateForecastTrendReportFilter,
    xaxis: false,
    composite: true,
    composite_transform: testRailsTestsEstimateForecastTrendReportCompositeTransform,
    chart_props: testRailsTestsEstimateForecastTrendReportChartProps,
    supported_filters: testrailsSupportedFilters,
    drilldown: testRailsDrilldown,
    transformFunction: (data: any) => trendReportTransformer(data)
  }
};

export default testRailsTestsEstimateForecastTrendReport;
