import { testrailsTransformer } from "custom-hooks/helpers/seriesData.helper";
import { testrailsSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { TestsEstimateForecastReportType } from "model/report/testRails/tests-estimate-forecast-report/testsEstimateForecastReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { testRailsDrilldown, testrailsStackFilters } from "../commonTestRailsReports.constants";
import { testRailsTestsEstimateForecastReportChartProps } from "./constants";
import { IntegrationTypes } from "constants/IntegrationTypes";

const testRailsTestsEstimateForecastReport: {
  testrails_tests_estimate_forecast_report: TestsEstimateForecastReportType;
} = {
  testrails_tests_estimate_forecast_report: {
    name: "TestRail Test Estimate Forecast Report",
    application: IntegrationTypes.TESTRAILS,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "testrails_tests_estimate_forecast_report",
    method: "list",
    filters: {},
    defaultFilterKey: "median",
    defaultAcross: "milestone",
    xaxis: true,
    chart_props: testRailsTestsEstimateForecastReportChartProps,
    stack_filters: testrailsStackFilters,
    supported_filters: testrailsSupportedFilters,
    drilldown: testRailsDrilldown,
    transformFunction: (data: any) => testrailsTransformer(data)
  }
};

export default testRailsTestsEstimateForecastReport;
