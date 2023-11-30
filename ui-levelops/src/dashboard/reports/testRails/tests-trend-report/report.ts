import { trendReportTransformer } from "custom-hooks/helpers";
import { REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import { testrailsSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { TestsTrendReportType } from "model/report/testRails/tests-trend-report/testsTrendReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { testRailsDrilldown } from "../commonTestRailsReports.constants";
import {
  testRailsTestsTrendReportChartProps,
  testRailsTestsTrendReportCompositeTransform,
  testRailsTestsTrendReportFilter
} from "./constants";
import { TestrailsTestTrendsReportFiltersConfig } from "./filter.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

const testRailsTestsTrendReport: { testrails_tests_trend_report: TestsTrendReportType } = {
  testrails_tests_trend_report: {
    name: "TestRail Test Trend Report",
    application: IntegrationTypes.TESTRAILS,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "testrails_tests_report",
    composite: true,
    composite_transform: testRailsTestsTrendReportCompositeTransform,
    method: "list",
    filters: testRailsTestsTrendReportFilter,
    xaxis: false,
    chart_props: testRailsTestsTrendReportChartProps,
    supported_filters: testrailsSupportedFilters,
    drilldown: testRailsDrilldown,
    transformFunction: (data: any) => trendReportTransformer(data),
    [REPORT_FILTERS_CONFIG]: TestrailsTestTrendsReportFiltersConfig
  }
};

export default testRailsTestsTrendReport;
