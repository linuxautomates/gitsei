import { testrailsTransformer } from "custom-hooks/helpers/seriesData.helper";
import { testrailsSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { TestsEstimateReportType } from "model/report/testRails/tests-estimate-report/testsEstimateReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { testRailsDrilldown, testrailsStackFilters } from "../commonTestRailsReports.constants";
import { testRailsTestsEstimateReportChartProps } from "./constants";
import { IntegrationTypes } from "constants/IntegrationTypes";

const testRailsTestsEstimateReport: { testrails_tests_estimate_report: TestsEstimateReportType } = {
  testrails_tests_estimate_report: {
    name: "TestRail Test Estimate Report",
    application: IntegrationTypes.TESTRAILS,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "testrails_tests_estimate_report",
    method: "list",
    filters: {},
    defaultFilterKey: "median",
    defaultAcross: "milestone",
    xaxis: true,
    chart_props: testRailsTestsEstimateReportChartProps,
    stack_filters: testrailsStackFilters,
    supported_filters: testrailsSupportedFilters,
    drilldown: testRailsDrilldown,
    transformFunction: data => testrailsTransformer(data)
  }
};
export default testRailsTestsEstimateReport;
