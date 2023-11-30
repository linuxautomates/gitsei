import { trendReportTransformer } from "custom-hooks/helpers";
import { testrailsSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { TestsEstimateTrendReportType } from "model/report/testRails/tests-estimate-trend-report/testsEstimateTrendReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { testRailsDrilldown } from "../commonTestRailsReports.constants";
import {
  testRailsTestsEstimateTrendReportChartProps,
  testRailsTestsEstimateTrendReportCompositeTransform,
  testRailsTestsEstimateTrendReportFilter
} from "./constants";
import { IntegrationTypes } from "constants/IntegrationTypes";

const testRailsTestsEstimateTrendReport: { testrails_tests_estimate_trend_report: TestsEstimateTrendReportType } = {
  testrails_tests_estimate_trend_report: {
    name: "TestRail Test Estimate Trend Report",
    application: IntegrationTypes.TESTRAILS,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "testrails_tests_estimate_report",
    method: "list",
    filters: testRailsTestsEstimateTrendReportFilter,
    xaxis: false,
    composite: true,
    composite_transform: testRailsTestsEstimateTrendReportCompositeTransform,
    chart_props: testRailsTestsEstimateTrendReportChartProps,
    supported_filters: testrailsSupportedFilters,
    drilldown: testRailsDrilldown,
    transformFunction: (data: any) => trendReportTransformer(data)
  }
};

export default testRailsTestsEstimateTrendReport;
